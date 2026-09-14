package kz.bilimedu.api.school;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SchoolClassRepository extends JpaRepository<SchoolClass, Long> {

    /**
     * Классы с учётом того, кому показываем (permissions.md):
     * ADMIN — все, TEACHER — где он ведёт хоть один предмет,
     * STUDENT — где он числится прямо сейчас.
     *
     * <p>Для ученика взята активная запись enrollments (to_date IS NULL),
     * а не «класс ученика»: такого поля нет намеренно — в версии 2023 года
     * Student.classId перезаписывался при переводе и переписывал историю.
     */
    @Query(value = """
            SELECT c.* FROM classes c
            WHERE (CAST(:academicYearId AS smallint) IS NULL
                   OR c.academic_year_id = CAST(:academicYearId AS smallint))
              AND (
                    CAST(:viewerRole AS text) = 'ADMIN'
                 OR (CAST(:viewerRole AS text) = 'TEACHER' AND EXISTS (
                        SELECT 1 FROM teaching_assignments a
                        WHERE a.class_id = c.id AND a.teacher_id = :viewerId))
                 OR (CAST(:viewerRole AS text) = 'STUDENT' AND EXISTS (
                        SELECT 1 FROM enrollments e
                        WHERE e.class_id = c.id AND e.student_id = :viewerId
                          AND e.to_date IS NULL))
              )
            ORDER BY c.academic_year_id DESC, c.name
            """,
            countQuery = """
            SELECT count(*) FROM classes c
            WHERE (CAST(:academicYearId AS smallint) IS NULL
                   OR c.academic_year_id = CAST(:academicYearId AS smallint))
              AND (
                    CAST(:viewerRole AS text) = 'ADMIN'
                 OR (CAST(:viewerRole AS text) = 'TEACHER' AND EXISTS (
                        SELECT 1 FROM teaching_assignments a
                        WHERE a.class_id = c.id AND a.teacher_id = :viewerId))
                 OR (CAST(:viewerRole AS text) = 'STUDENT' AND EXISTS (
                        SELECT 1 FROM enrollments e
                        WHERE e.class_id = c.id AND e.student_id = :viewerId
                          AND e.to_date IS NULL))
              )
            """,
            nativeQuery = true)
    Page<SchoolClass> findVisible(@Param("viewerId") Long viewerId,
                                  @Param("viewerRole") String viewerRole,
                                  @Param("academicYearId") Short academicYearId,
                                  Pageable pageable);
}
