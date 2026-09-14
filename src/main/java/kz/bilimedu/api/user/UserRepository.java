package kz.bilimedu.api.user;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Колонка email имеет тип citext, но JDBC-драйвер передаёт параметр как
     * varchar — при таком сочетании PostgreSQL сравнивает строки с учётом
     * регистра. Явное приведение возвращает регистронезависимое сравнение
     * и по-прежнему использует уникальный индекс по email.
     */
    @Query(value = "SELECT * FROM users WHERE email = CAST(:email AS citext)", nativeQuery = true)
    Optional<User> findByEmail(@Param("email") String email);

    @Query(value = "SELECT EXISTS (SELECT 1 FROM users WHERE email = CAST(:email AS citext))",
            nativeQuery = true)
    boolean emailTaken(@Param("email") String email);

    boolean existsByRoleAndDeactivatedAtIsNull(Role role);

    /**
     * Список пользователей с учётом того, кому он показывается.
     *
     * <p>Видимость — часть запроса, а не фильтр поверх результата: иначе
     * общее число страниц выдавало бы существование записей, которых
     * запрашивающий видеть не должен.
     *
     * <ul>
     *   <li>ADMIN — все;
     *   <li>TEACHER — коллеги-учителя и ученики своих классов;
     *   <li>STUDENT — только он сам.
     * </ul>
     *
     * <p>Принадлежность ученика классу берётся из enrollments, а не из поля
     * в записи ученика: в версии 2023 года перевод переписывал Student.classId,
     * из-за чего прошлогодние оценки начинали выглядеть как оценки нового класса.
     */
    @Query(value = """
            SELECT u.* FROM users u
            WHERE (CAST(:role AS text) IS NULL OR CAST(u.role AS text) = CAST(:role AS text))
              AND (CAST(:classId AS bigint) IS NULL OR EXISTS (
                    SELECT 1 FROM enrollments e
                    WHERE e.student_id = u.id
                      AND e.class_id = CAST(:classId AS bigint)
                      AND e.to_date IS NULL))
              AND (:includeDeactivated = TRUE OR u.deactivated_at IS NULL)
              AND (
                    CAST(:viewerRole AS text) = 'ADMIN'
                 OR u.id = :viewerId
                 OR (CAST(:viewerRole AS text) = 'TEACHER' AND (
                        CAST(u.role AS text) = 'TEACHER'
                     OR EXISTS (
                            SELECT 1 FROM enrollments e
                            JOIN teaching_assignments a ON a.class_id = e.class_id
                            WHERE e.student_id = u.id AND a.teacher_id = :viewerId)))
              )
            ORDER BY u.full_name, u.id
            """,
            countQuery = """
            SELECT count(*) FROM users u
            WHERE (CAST(:role AS text) IS NULL OR CAST(u.role AS text) = CAST(:role AS text))
              AND (CAST(:classId AS bigint) IS NULL OR EXISTS (
                    SELECT 1 FROM enrollments e
                    WHERE e.student_id = u.id
                      AND e.class_id = CAST(:classId AS bigint)
                      AND e.to_date IS NULL))
              AND (:includeDeactivated = TRUE OR u.deactivated_at IS NULL)
              AND (
                    CAST(:viewerRole AS text) = 'ADMIN'
                 OR u.id = :viewerId
                 OR (CAST(:viewerRole AS text) = 'TEACHER' AND (
                        CAST(u.role AS text) = 'TEACHER'
                     OR EXISTS (
                            SELECT 1 FROM enrollments e
                            JOIN teaching_assignments a ON a.class_id = e.class_id
                            WHERE e.student_id = u.id AND a.teacher_id = :viewerId)))
              )
            """,
            nativeQuery = true)
    Page<User> findVisible(@Param("viewerId") Long viewerId,
                           @Param("viewerRole") String viewerRole,
                           @Param("role") String role,
                           @Param("classId") Long classId,
                           @Param("includeDeactivated") boolean includeDeactivated,
                           Pageable pageable);

    /** Видит ли учитель этого ученика — тот же предикат для одиночного чтения. */
    @Query(value = """
            SELECT EXISTS (
                SELECT 1 FROM enrollments e
                JOIN teaching_assignments a ON a.class_id = e.class_id
                WHERE e.student_id = :studentId AND a.teacher_id = :teacherId)
            """, nativeQuery = true)
    boolean teacherTeachesStudent(@Param("teacherId") Long teacherId, @Param("studentId") Long studentId);
}
