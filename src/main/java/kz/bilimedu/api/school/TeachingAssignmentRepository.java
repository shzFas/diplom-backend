package kz.bilimedu.api.school;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TeachingAssignmentRepository extends JpaRepository<TeachingAssignment, Long> {

    /** ADMIN видит все назначения, TEACHER — только свои. */
    @Query(value = """
            SELECT a.* FROM teaching_assignments a
            WHERE (CAST(:teacherId AS bigint) IS NULL OR a.teacher_id = CAST(:teacherId AS bigint))
              AND (CAST(:classId AS bigint) IS NULL OR a.class_id = CAST(:classId AS bigint))
              AND (CAST(:viewerRole AS text) = 'ADMIN' OR a.teacher_id = :viewerId)
            ORDER BY a.class_id, a.subject_id
            """,
            countQuery = """
            SELECT count(*) FROM teaching_assignments a
            WHERE (CAST(:teacherId AS bigint) IS NULL OR a.teacher_id = CAST(:teacherId AS bigint))
              AND (CAST(:classId AS bigint) IS NULL OR a.class_id = CAST(:classId AS bigint))
              AND (CAST(:viewerRole AS text) = 'ADMIN' OR a.teacher_id = :viewerId)
            """,
            nativeQuery = true)
    Page<TeachingAssignment> findVisible(@Param("viewerId") Long viewerId,
                                         @Param("viewerRole") String viewerRole,
                                         @Param("teacherId") Long teacherId,
                                         @Param("classId") Long classId,
                                         Pageable pageable);

    /**
     * Уроки ссылаются на назначение с ON DELETE CASCADE: удаление назначения
     * унесло бы журнал вместе с оценками. Поэтому перед удалением проверяем,
     * что уроков нет.
     */
    @Query(value = "SELECT EXISTS (SELECT 1 FROM lessons WHERE assignment_id = :id)", nativeQuery = true)
    boolean hasLessons(@Param("id") Long id);
}
