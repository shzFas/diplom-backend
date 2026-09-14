package kz.bilimedu.api.lesson;

import kz.bilimedu.api.common.ApiException;
import kz.bilimedu.api.common.ErrorCode;
import kz.bilimedu.api.common.PageResponse;
import kz.bilimedu.api.lesson.dto.CreateLessonRequest;
import kz.bilimedu.api.lesson.dto.LessonResponse;
import kz.bilimedu.api.lesson.dto.UpdateLessonRequest;
import kz.bilimedu.api.school.TeachingAssignment;
import kz.bilimedu.api.school.TeachingAssignmentRepository;
import kz.bilimedu.api.security.AuthenticatedUser;
import kz.bilimedu.api.user.Role;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Планирование и журнал. Уроки заводит учитель — и только в своих
 * назначениях; завуч их читает, но не создаёт, как и не правит оценки.
 *
 * <p>Правила «дата урока внутри своей четверти» (D3) и «один СОЧ на
 * четверть» (D5) не проверяются запросом перед вставкой: их держат триггер
 * и частичный уникальный индекс. В версии 2023 года такая проверка
 * загружала всю коллекцию в память Node и фильтровала её через .filter(),
 * из-за чего два параллельных запроса оба не находили дубль и оба писали.
 */
@Service
public class LessonService {

    private final LessonRepository lessons;
    private final TeachingAssignmentRepository assignments;

    public LessonService(LessonRepository lessons, TeachingAssignmentRepository assignments) {
        this.lessons = lessons;
        this.assignments = assignments;
    }

    @Transactional(readOnly = true)
    public PageResponse<LessonResponse> list(AuthenticatedUser viewer, Long assignmentId,
                                             Short termId, Pageable pageable) {
        return PageResponse.of(lessons
                .findVisible(viewer.id(), viewer.role().name(), assignmentId, termId, pageable)
                .map(LessonResponse::from));
    }

    @Transactional(readOnly = true)
    public LessonResponse get(AuthenticatedUser viewer, Long id) {
        Lesson lesson = lessons.findById(id).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        if (!canRead(viewer, lesson)) {
            throw new ApiException(ErrorCode.NOT_OWNER);
        }
        return LessonResponse.from(lesson);
    }

    @Transactional
    public LessonResponse create(AuthenticatedUser teacher, CreateLessonRequest request) {
        requireOwnAssignment(teacher, request.assignmentId());

        boolean sameYear = lessons
                .termMatchesAssignmentYear(request.assignmentId(), request.termId())
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        if (!sameYear) {
            throw new ApiException(ErrorCode.TERM_YEAR_MISMATCH);
        }

        Lesson created = lessons.save(new Lesson(
                request.assignmentId(),
                request.termId(),
                request.title().strip(),
                request.lessonDate(),
                request.kind(),
                request.maxScore()));
        return LessonResponse.from(created);
    }

    @Transactional
    public LessonResponse update(AuthenticatedUser teacher, Long id, UpdateLessonRequest request) {
        Lesson lesson = lessons.findById(id).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        requireOwnAssignment(teacher, lesson.getAssignmentId());

        if (request.title() != null) {
            lesson.rename(request.title().strip());
        }
        if (request.lessonDate() != null) {
            // Попадание новой даты в четверть урока проверит триггер
            // lesson_date_within_term и вернёт LESSON_OUTSIDE_TERM.
            lesson.moveTo(request.lessonDate());
        }
        if (request.maxScore() != null) {
            lesson.changeMaxScore(request.maxScore());
        }
        return LessonResponse.from(lessons.save(lesson));
    }

    /** Удаление урока каскадом уносит его оценки — так задумано в контракте. */
    @Transactional
    public void delete(AuthenticatedUser teacher, Long id) {
        Lesson lesson = lessons.findById(id).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        requireOwnAssignment(teacher, lesson.getAssignmentId());
        lessons.delete(lesson);
    }

    /** Предикат P1: учитель распоряжается только своими назначениями. */
    private void requireOwnAssignment(AuthenticatedUser teacher, Long assignmentId) {
        TeachingAssignment assignment = assignments.findById(assignmentId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        if (!assignment.getTeacherId().equals(teacher.id())) {
            throw new ApiException(ErrorCode.NOT_OWNER);
        }
    }

    private boolean canRead(AuthenticatedUser viewer, Lesson lesson) {
        return switch (viewer.role()) {
            case ADMIN -> true;
            case TEACHER -> assignments.findById(lesson.getAssignmentId())
                    .map(assignment -> assignment.getTeacherId().equals(viewer.id()))
                    .orElse(false);
            // Предикат P3: ученик видит урок, если числился в классе на его дату.
            case STUDENT -> lessons.studentSawLesson(viewer.id(), lesson.getId());
        };
    }
}
