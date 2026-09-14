package kz.bilimedu.api.lesson;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LessonRepository extends JpaRepository<Lesson, Long> {

    /**
     * Уроки с учётом того, кому показываем.
     *
     * <ul>
     *   <li>ADMIN — все;
     *   <li>TEACHER — уроки своих назначений (предикат P1);
     *   <li>STUDENT — уроки класса, в котором он числился <b>на дату урока</b>
     *       (предикат P3).
     * </ul>
     *
     * <p>P3 — не педантизм: переведённый ученик должен видеть сентябрьские
     * уроки прежнего класса и не должен видеть сентябрьские уроки нового.
     * Проверка по текущему классу дала бы оба ответа неверно.
     */
    @Query(value = """
            SELECT l.* FROM lessons l
            JOIN teaching_assignments a ON a.id = l.assignment_id
            WHERE (CAST(:assignmentId AS bigint) IS NULL
                   OR l.assignment_id = CAST(:assignmentId AS bigint))
              AND (CAST(:termId AS smallint) IS NULL OR l.term_id = CAST(:termId AS smallint))
              AND (
                    CAST(:viewerRole AS text) = 'ADMIN'
                 OR (CAST(:viewerRole AS text) = 'TEACHER' AND a.teacher_id = :viewerId)
                 OR (CAST(:viewerRole AS text) = 'STUDENT' AND EXISTS (
                        SELECT 1 FROM enrollments e
                        WHERE e.student_id = :viewerId
                          AND e.class_id = a.class_id
                          AND l.lesson_date BETWEEN e.from_date
                                                AND COALESCE(e.to_date, 'infinity'::date)))
              )
            ORDER BY l.lesson_date, l.id
            """,
            countQuery = """
            SELECT count(*) FROM lessons l
            JOIN teaching_assignments a ON a.id = l.assignment_id
            WHERE (CAST(:assignmentId AS bigint) IS NULL
                   OR l.assignment_id = CAST(:assignmentId AS bigint))
              AND (CAST(:termId AS smallint) IS NULL OR l.term_id = CAST(:termId AS smallint))
              AND (
                    CAST(:viewerRole AS text) = 'ADMIN'
                 OR (CAST(:viewerRole AS text) = 'TEACHER' AND a.teacher_id = :viewerId)
                 OR (CAST(:viewerRole AS text) = 'STUDENT' AND EXISTS (
                        SELECT 1 FROM enrollments e
                        WHERE e.student_id = :viewerId
                          AND e.class_id = a.class_id
                          AND l.lesson_date BETWEEN e.from_date
                                                AND COALESCE(e.to_date, 'infinity'::date)))
              )
            """,
            nativeQuery = true)
    Page<Lesson> findVisible(@Param("viewerId") Long viewerId,
                             @Param("viewerRole") String viewerRole,
                             @Param("assignmentId") Long assignmentId,
                             @Param("termId") Short termId,
                             Pageable pageable);

    /** Тот же предикат для одиночного чтения. */
    @Query(value = """
            SELECT EXISTS (
                SELECT 1 FROM lessons l
                JOIN teaching_assignments a ON a.id = l.assignment_id
                WHERE l.id = :lessonId
                  AND EXISTS (
                        SELECT 1 FROM enrollments e
                        WHERE e.student_id = :studentId
                          AND e.class_id = a.class_id
                          AND l.lesson_date BETWEEN e.from_date
                                                AND COALESCE(e.to_date, 'infinity'::date)))
            """, nativeQuery = true)
    boolean studentSawLesson(@Param("studentId") Long studentId, @Param("lessonId") Long lessonId);

    /**
     * Учебный год класса, которому принадлежит назначение, и учебный год
     * четверти — чтобы не дать повесить урок класса 2025/26 на четверть
     * следующего года. Схема такую связь не выражает: lessons ссылается
     * на term и на assignment независимо друг от друга.
     */
    @Query(value = """
            SELECT c.academic_year_id = t.academic_year_id
            FROM teaching_assignments a
            JOIN classes c ON c.id = a.class_id
            JOIN terms t   ON t.id = :termId
            WHERE a.id = :assignmentId
            """, nativeQuery = true)
    Optional<Boolean> termMatchesAssignmentYear(@Param("assignmentId") Long assignmentId,
                                                @Param("termId") Short termId);
}
