package kz.bilimedu.api.grade;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import kz.bilimedu.api.calendar.Term;
import kz.bilimedu.api.calendar.TermRepository;
import kz.bilimedu.api.common.ApiException;
import kz.bilimedu.api.common.ErrorCode;
import kz.bilimedu.api.common.PageResponse;
import kz.bilimedu.api.grade.dto.DeleteGradeRequest;
import kz.bilimedu.api.grade.dto.GradeAuditResponse;
import kz.bilimedu.api.grade.dto.GradeEntryRequest;
import kz.bilimedu.api.grade.dto.GradeResponse;
import kz.bilimedu.api.grade.dto.JournalEntryResponse;
import kz.bilimedu.api.grade.dto.StudentGradeResponse;
import kz.bilimedu.api.grade.dto.UpdateGradeRequest;
import kz.bilimedu.api.lesson.Lesson;
import kz.bilimedu.api.lesson.LessonRepository;
import kz.bilimedu.api.school.TeachingAssignment;
import kz.bilimedu.api.school.TeachingAssignmentRepository;
import kz.bilimedu.api.security.AuthenticatedUser;
import kz.bilimedu.api.user.Role;
import kz.bilimedu.api.user.UserRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Оценки, журнал и аудит правок.
 *
 * <p>Здесь сходятся правила, которых версия 2023 года не проверяла вовсе:
 * оценку ставит только назначенный на класс и предмет учитель (D8), только
 * ученику, числившемуся в классе на дату урока (D9), только в открытой
 * четверти (D4), не более одной на урок (D6) и не выше максимума урока (D7).
 * Последние два держит база, остальные — этот сервис.
 */
@Service
public class GradeService {

    private final GradeRepository grades;
    private final GradeAuditRepository audits;
    private final LessonRepository lessons;
    private final TeachingAssignmentRepository assignments;
    private final TermRepository terms;
    private final UserRepository users;

    public GradeService(GradeRepository grades,
                        GradeAuditRepository audits,
                        LessonRepository lessons,
                        TeachingAssignmentRepository assignments,
                        TermRepository terms,
                        UserRepository users) {
        this.grades = grades;
        this.audits = audits;
        this.lessons = lessons;
        this.assignments = assignments;
        this.terms = terms;
        this.users = users;
    }

    /** Журнал по уроку. Ученику не показывается: он читает только свои оценки. */
    @Transactional(readOnly = true)
    public PageResponse<JournalEntryResponse> journal(AuthenticatedUser viewer, Long lessonId,
                                                      Pageable pageable) {
        Lesson lesson = requireLesson(lessonId);
        TeachingAssignment assignment = requireAssignment(lesson.getAssignmentId());

        if (viewer.role() == Role.TEACHER && !assignment.getTeacherId().equals(viewer.id())) {
            throw new ApiException(ErrorCode.NOT_OWNER);
        }
        return PageResponse.of(grades
                .journal(lessonId, assignment.getClassId(), lesson.getLessonDate(), pageable)
                .map(JournalEntryResponse::from));
    }

    /**
     * Выставление оценок списком — замена поштучного POST /marks: учитель
     * закрывает журнал класса одним действием.
     *
     * <p>Идемпотентно в строгом смысле: повторная отправка того же тела не
     * меняет ни оценок, ни аудита. Балл, совпавший с уже выставленным, не
     * порождает запись UPDATED — иначе аудит забился бы шумом от повторных
     * нажатий кнопки «сохранить».
     *
     * <p>Отсутствующие в теле ученики <b>не</b> теряют оценки: удаление
     * требует причины (она попадает в аудит), а в теле PUT её взять негде.
     * Удаление оформляется отдельным DELETE /grades/{id} с причиной.
     */
    @Transactional
    public List<GradeResponse> putGrades(AuthenticatedUser teacher, Long lessonId,
                                         List<GradeEntryRequest> entries) {
        Lesson lesson = requireLesson(lessonId);
        requireOwnAssignment(teacher, lesson.getAssignmentId());
        requireOpenTerm(lesson);

        Map<Long, Short> requested = new LinkedHashMap<>();
        Set<Long> duplicates = new LinkedHashSet<>();
        for (GradeEntryRequest entry : entries) {
            if (requested.put(entry.studentId(), entry.score()) != null) {
                duplicates.add(entry.studentId());
            }
        }
        if (!duplicates.isEmpty()) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, details("studentId", duplicates));
        }

        // Правило D9 проверяется для всех сразу: частично применённый журнал
        // хуже отклонённого — учитель не увидит, что часть класса не прошла.
        Set<Long> notEnrolled = new LinkedHashSet<>();
        for (Long studentId : requested.keySet()) {
            if (!lessons.studentEnrolledOnLessonDate(studentId, lessonId)) {
                notEnrolled.add(studentId);
            }
        }
        if (!notEnrolled.isEmpty()) {
            throw new ApiException(ErrorCode.STUDENT_NOT_ENROLLED, details("studentId", notEnrolled));
        }

        List<GradeResponse> result = new ArrayList<>(requested.size());
        for (var entry : requested.entrySet()) {
            result.add(GradeResponse.from(upsert(teacher, lessonId, entry.getKey(), entry.getValue())));
        }
        return result;
    }

    @Transactional
    public GradeResponse update(AuthenticatedUser teacher, Long gradeId, UpdateGradeRequest request) {
        Grade grade = grades.findById(gradeId).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        Lesson lesson = requireLesson(grade.getLessonId());
        requireOwnAssignment(teacher, lesson.getAssignmentId());
        requireOpenTerm(lesson);

        Short oldScore = grade.getScore();
        if (!oldScore.equals(request.score())) {
            grade.rescore(request.score(), teacher.id());
            grades.save(grade);
            audits.save(GradeAudit.updated(grade, oldScore, teacher.id(), request.reason()));
        }
        return GradeResponse.from(grade);
    }

    @Transactional
    public void delete(AuthenticatedUser teacher, Long gradeId, DeleteGradeRequest request) {
        Grade grade = grades.findById(gradeId).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        Lesson lesson = requireLesson(grade.getLessonId());
        requireOwnAssignment(teacher, lesson.getAssignmentId());
        requireOpenTerm(lesson);

        // Аудит пишется до удаления, но переживёт его: grade_audit.grade_id
        // объявлен без внешнего ключа именно для этого (правило D17).
        audits.save(GradeAudit.deleted(grade, teacher.id(), request.reason()));
        grades.delete(grade);
    }

    @Transactional(readOnly = true)
    public PageResponse<GradeAuditResponse> audit(AuthenticatedUser viewer, Long gradeId,
                                                  Pageable pageable) {
        // Оценки может уже не быть — история её правок всё равно доступна.
        grades.findById(gradeId).ifPresentOrElse(
                grade -> requireAuditAccess(viewer, grade),
                () -> requireAuditAccessForDeleted(viewer, gradeId));

        return PageResponse.of(audits
                .findByGradeIdOrderByAtDescIdDesc(gradeId, pageable)
                .map(GradeAuditResponse::from));
    }

    @Transactional(readOnly = true)
    public PageResponse<StudentGradeResponse> studentDiary(AuthenticatedUser viewer, Long studentId,
                                                           Short termId, Long subjectId,
                                                           Pageable pageable) {
        if (!canReadStudent(viewer, studentId)) {
            throw new ApiException(ErrorCode.NOT_OWNER);
        }
        return PageResponse.of(grades
                .studentDiary(studentId, termId, subjectId, pageable)
                .map(StudentGradeResponse::from));
    }

    private Grade upsert(AuthenticatedUser teacher, Long lessonId, Long studentId, Short score) {
        Grade existing = grades.findByLessonIdAndStudentId(lessonId, studentId).orElse(null);

        if (existing == null) {
            Grade created = grades.save(new Grade(lessonId, studentId, score, teacher.id()));
            audits.save(GradeAudit.created(created, teacher.id()));
            return created;
        }
        if (existing.getScore().equals(score)) {
            return existing;
        }
        Short oldScore = existing.getScore();
        existing.rescore(score, teacher.id());
        grades.save(existing);
        audits.save(GradeAudit.updated(existing, oldScore, teacher.id(), null));
        return existing;
    }

    /** Правило D4: закрытая четверть не принимает новые и изменённые оценки. */
    private void requireOpenTerm(Lesson lesson) {
        Term term = terms.findById(lesson.getTermId())
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        if (term.isClosed()) {
            throw new ApiException(ErrorCode.TERM_CLOSED);
        }
    }

    /** Правило D8 и предикат P1: оценку ставит только назначенный учитель. */
    private void requireOwnAssignment(AuthenticatedUser teacher, Long assignmentId) {
        if (!requireAssignment(assignmentId).getTeacherId().equals(teacher.id())) {
            throw new ApiException(ErrorCode.NOT_OWNER);
        }
    }

    private Lesson requireLesson(Long lessonId) {
        return lessons.findById(lessonId).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
    }

    private TeachingAssignment requireAssignment(Long assignmentId) {
        return assignments.findById(assignmentId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
    }

    private void requireAuditAccess(AuthenticatedUser viewer, Grade grade) {
        if (viewer.role() == Role.ADMIN) {
            return;
        }
        Lesson lesson = requireLesson(grade.getLessonId());
        requireOwnAssignment(viewer, lesson.getAssignmentId());
    }

    /**
     * Аудит удалённой оценки. Связи с уроком уже нет, поэтому принадлежность
     * определяется по самой записи аудита: учитель видит историю тех оценок,
     * которые правил сам.
     */
    private void requireAuditAccessForDeleted(AuthenticatedUser viewer, Long gradeId) {
        if (viewer.role() == Role.ADMIN) {
            return;
        }
        boolean ownAction = audits.findByGradeIdOrderByAtDescIdDesc(gradeId, Pageable.ofSize(1))
                .stream()
                .anyMatch(audit -> audit.getActorId().equals(viewer.id()));
        if (!ownAction) {
            throw new ApiException(ErrorCode.NOT_OWNER);
        }
    }

    private boolean canReadStudent(AuthenticatedUser viewer, Long studentId) {
        return switch (viewer.role()) {
            case ADMIN -> true;
            case STUDENT -> viewer.id().equals(studentId);
            case TEACHER -> users.teacherTeachesStudent(viewer.id(), studentId);
        };
    }

    private List<Map<String, Object>> details(String field, Set<Long> ids) {
        return ids.stream()
                .map(id -> Map.<String, Object>of("field", field, "value", String.valueOf(id)))
                .toList();
    }
}
