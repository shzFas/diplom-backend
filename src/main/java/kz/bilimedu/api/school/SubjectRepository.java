package kz.bilimedu.api.school;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubjectRepository extends JpaRepository<Subject, Long> {

    /**
     * ADMIN и TEACHER видят все предметы, STUDENT — только те, что ведутся
     * в его текущем классе. Фильтр ?classId= сужает список до предметов
     * конкретного класса — заменяет GET /predmet/class/:id, где фильтр лежал
     * в сегменте пути.
     */
    @Query(value = """
            SELECT s.* FROM subjects s
            WHERE (CAST(:classId AS bigint) IS NULL OR EXISTS (
                    SELECT 1 FROM teaching_assignments a
                    WHERE a.subject_id = s.id AND a.class_id = CAST(:classId AS bigint)))
              AND (
                    CAST(:viewerRole AS text) IN ('ADMIN', 'TEACHER')
                 OR EXISTS (
                        SELECT 1 FROM teaching_assignments a
                        JOIN enrollments e ON e.class_id = a.class_id
                        WHERE a.subject_id = s.id AND e.student_id = :viewerId
                          AND e.to_date IS NULL)
              )
            ORDER BY s.name
            """,
            countQuery = """
            SELECT count(*) FROM subjects s
            WHERE (CAST(:classId AS bigint) IS NULL OR EXISTS (
                    SELECT 1 FROM teaching_assignments a
                    WHERE a.subject_id = s.id AND a.class_id = CAST(:classId AS bigint)))
              AND (
                    CAST(:viewerRole AS text) IN ('ADMIN', 'TEACHER')
                 OR EXISTS (
                        SELECT 1 FROM teaching_assignments a
                        JOIN enrollments e ON e.class_id = a.class_id
                        WHERE a.subject_id = s.id AND e.student_id = :viewerId
                          AND e.to_date IS NULL)
              )
            """,
            nativeQuery = true)
    Page<Subject> findVisible(@Param("viewerId") Long viewerId,
                              @Param("viewerRole") String viewerRole,
                              @Param("classId") Long classId,
                              Pageable pageable);
}
