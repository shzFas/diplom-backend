package kz.bilimedu.api.grade;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GradeRepository extends JpaRepository<Grade, Long> {

    Optional<Grade> findByLessonIdAndStudentId(Long lessonId, Long studentId);

    List<Grade> findByLessonId(Long lessonId);

    /**
     * Журнал по уроку: состав класса на дату урока плюс оценки.
     *
     * <p>Список учеников берётся из enrollments по дате урока, а не по
     * текущему составу класса: иначе в журнале сентябрьского урока
     * оказались бы те, кто пришёл в класс в январе, и не оказалось бы тех,
     * кто в сентябре в нём учился.
     */
    @Query("""
            SELECT new kz.bilimedu.api.grade.JournalRow(u.id, u.fullName, g.id, g.score)
            FROM Enrollment e
            JOIN User u ON u.id = e.studentId
            LEFT JOIN Grade g ON g.studentId = u.id AND g.lessonId = :lessonId
            WHERE e.classId = :classId
              AND e.fromDate <= :lessonDate
              AND (e.toDate IS NULL OR e.toDate >= :lessonDate)
            ORDER BY u.fullName, u.id
            """)
    Page<JournalRow> journal(@Param("lessonId") Long lessonId,
                             @Param("classId") Long classId,
                             @Param("lessonDate") java.time.LocalDate lessonDate,
                             Pageable pageable);

    /** Дневник ученика: оценки с уроком, предметом и четвертью. */
    @Query("""
            SELECT new kz.bilimedu.api.grade.StudentGradeRow(
                   g.id, l.id, l.title, l.lessonDate, l.kind, g.score, l.maxScore,
                   s.id, s.name, l.termId)
            FROM Grade g
            JOIN Lesson l ON l.id = g.lessonId
            JOIN TeachingAssignment a ON a.id = l.assignmentId
            JOIN Subject s ON s.id = a.subjectId
            WHERE g.studentId = :studentId
              AND (:termId IS NULL OR l.termId = :termId)
              AND (:subjectId IS NULL OR a.subjectId = :subjectId)
            ORDER BY l.lessonDate DESC, g.id DESC
            """)
    Page<StudentGradeRow> studentDiary(@Param("studentId") Long studentId,
                                       @Param("termId") Short termId,
                                       @Param("subjectId") Long subjectId,
                                       Pageable pageable);

    /**
     * Правило D9: ученику можно поставить оценку, только если он числился
     * в классе на дату урока. Запрос взят из domain-rules.md дословно.
     */
    @Query(value = """
            SELECT EXISTS (
                SELECT 1 FROM enrollments e
                JOIN teaching_assignments a ON a.class_id = e.class_id
                JOIN lessons l              ON l.assignment_id = a.id
                WHERE e.student_id = :studentId
                  AND l.id         = :lessonId
                  AND l.lesson_date BETWEEN e.from_date
                                        AND COALESCE(e.to_date, 'infinity'::date))
            """, nativeQuery = true)
    boolean studentEnrolledOnLessonDate(@Param("studentId") Long studentId,
                                        @Param("lessonId") Long lessonId);
}
