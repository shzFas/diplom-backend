package kz.bilimedu.api.attendance;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import kz.bilimedu.api.attendance.dto.AttendanceEntryRequest;
import kz.bilimedu.api.attendance.dto.AttendanceResponse;
import kz.bilimedu.api.attendance.dto.LessonAttendanceEntryResponse;
import kz.bilimedu.api.attendance.dto.StudentAttendanceResponse;
import kz.bilimedu.api.common.ApiException;
import kz.bilimedu.api.common.ErrorCode;
import kz.bilimedu.api.common.PageResponse;
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
 * Посещаемость. Ведёт учитель — и только в своих назначениях, как и оценки.
 *
 * <p>Домен отдельный от оценок (правило D10): отсутствие не отменяет балл
 * за отработку, а присутствие не подразумевает оценку. Поэтому отметок
 * и оценок может не быть в любом сочетании, и одна не выводится из другой.
 *
 * <p>Аудита у посещаемости нет: схема его не предусматривает, и в отличие
 * от оценки отметка не является юридически значимой записью.
 */
@Service
public class AttendanceService {

    private final AttendanceRepository attendance;
    private final LessonRepository lessons;
    private final TeachingAssignmentRepository assignments;
    private final UserRepository users;

    public AttendanceService(AttendanceRepository attendance,
                             LessonRepository lessons,
                             TeachingAssignmentRepository assignments,
                             UserRepository users) {
        this.attendance = attendance;
        this.lessons = lessons;
        this.assignments = assignments;
        this.users = users;
    }

    /** Ведомость по уроку. Ученику не показывается: он читает только свою посещаемость. */
    @Transactional(readOnly = true)
    public PageResponse<LessonAttendanceEntryResponse> lessonSheet(AuthenticatedUser viewer,
                                                                   Long lessonId,
                                                                   Pageable pageable) {
        Lesson lesson = requireLesson(lessonId);
        TeachingAssignment assignment = requireAssignment(lesson.getAssignmentId());

        if (viewer.role() == Role.TEACHER && !assignment.getTeacherId().equals(viewer.id())) {
            throw new ApiException(ErrorCode.NOT_OWNER);
        }
        return PageResponse.of(attendance
                .lessonSheet(lessonId, assignment.getClassId(), lesson.getLessonDate(), pageable)
                .map(LessonAttendanceEntryResponse::from));
    }

    /**
     * Отметки списком: учитель закрывает ведомость класса одним действием.
     * Идемпотентно — статус, совпавший с уже отмеченным, не порождает записи.
     */
    @Transactional
    public List<AttendanceResponse> putAttendance(AuthenticatedUser teacher, Long lessonId,
                                                  List<AttendanceEntryRequest> entries) {
        Lesson lesson = requireLesson(lessonId);
        requireOwnAssignment(teacher, lesson.getAssignmentId());

        Map<Long, AttendanceStatus> requested = new LinkedHashMap<>();
        Set<Long> duplicates = new LinkedHashSet<>();
        for (AttendanceEntryRequest entry : entries) {
            if (requested.put(entry.studentId(), entry.status()) != null) {
                duplicates.add(entry.studentId());
            }
        }
        if (!duplicates.isEmpty()) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, details(duplicates));
        }

        // Тот же предикат, что у оценок: отметить можно только того, кто
        // числился в классе на дату урока. Ведомость применяется целиком.
        Set<Long> notEnrolled = new LinkedHashSet<>();
        for (Long studentId : requested.keySet()) {
            if (!lessons.studentEnrolledOnLessonDate(studentId, lessonId)) {
                notEnrolled.add(studentId);
            }
        }
        if (!notEnrolled.isEmpty()) {
            throw new ApiException(ErrorCode.STUDENT_NOT_ENROLLED, details(notEnrolled));
        }

        List<AttendanceResponse> result = new ArrayList<>(requested.size());
        for (var entry : requested.entrySet()) {
            result.add(AttendanceResponse.from(upsert(teacher, lessonId, entry.getKey(), entry.getValue())));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public PageResponse<StudentAttendanceResponse> studentSummary(AuthenticatedUser viewer,
                                                                  Long studentId, Short termId,
                                                                  Pageable pageable) {
        if (!canReadStudent(viewer, studentId)) {
            throw new ApiException(ErrorCode.NOT_OWNER);
        }
        return PageResponse.of(attendance
                .studentSummary(studentId, termId, pageable)
                .map(StudentAttendanceResponse::from));
    }

    private Attendance upsert(AuthenticatedUser teacher, Long lessonId, Long studentId,
                              AttendanceStatus status) {
        Attendance existing = attendance.findByLessonIdAndStudentId(lessonId, studentId).orElse(null);
        if (existing == null) {
            return attendance.save(new Attendance(lessonId, studentId, status, teacher.id()));
        }
        if (existing.getStatus() != status) {
            existing.mark(status, teacher.id());
            attendance.save(existing);
        }
        return existing;
    }

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

    private boolean canReadStudent(AuthenticatedUser viewer, Long studentId) {
        return switch (viewer.role()) {
            case ADMIN -> true;
            case STUDENT -> viewer.id().equals(studentId);
            case TEACHER -> users.teacherTeachesStudent(viewer.id(), studentId);
        };
    }

    private List<Map<String, Object>> details(Set<Long> ids) {
        return ids.stream()
                .map(id -> Map.<String, Object>of("field", "studentId", "value", String.valueOf(id)))
                .toList();
    }
}
