package kz.bilimedu.api.enrollment;

import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    Optional<Enrollment> findByStudentIdAndToDateIsNull(Long studentId);

    /**
     * Состав класса на произвольную дату. Именно здесь окупается временна́я
     * модель: отдельной таблицы истории нет, а состав на любой прошлый день
     * получается сравнением даты с интервалом зачисления.
     */
    @Query("""
            SELECT new kz.bilimedu.api.enrollment.RosterRow(
                   u.id, u.fullName, u.email, e.fromDate, e.toDate)
            FROM Enrollment e, User u
            WHERE u.id = e.studentId
              AND e.classId = :classId
              AND e.fromDate <= :on
              AND (e.toDate IS NULL OR e.toDate >= :on)
            ORDER BY u.fullName, u.id
            """)
    Page<RosterRow> roster(@Param("classId") Long classId, @Param("on") LocalDate on, Pageable pageable);

    /** История переводов ученика — от свежих к старым. */
    @Query("""
            SELECT new kz.bilimedu.api.enrollment.EnrollmentRow(
                   e.id, e.studentId, e.classId, c.name, c.academicYearId, e.fromDate, e.toDate)
            FROM Enrollment e, SchoolClass c
            WHERE c.id = e.classId AND e.studentId = :studentId
            ORDER BY e.fromDate DESC, e.id DESC
            """)
    Page<EnrollmentRow> history(@Param("studentId") Long studentId, Pageable pageable);

    @Query("""
            SELECT new kz.bilimedu.api.enrollment.EnrollmentRow(
                   e.id, e.studentId, e.classId, c.name, c.academicYearId, e.fromDate, e.toDate)
            FROM Enrollment e, SchoolClass c
            WHERE c.id = e.classId AND e.id = :id
            """)
    Optional<EnrollmentRow> row(@Param("id") Long id);

    /** Предикат P1 в применении к классу: ведёт ли учитель в этом классе хоть один предмет. */
    @Query(value = "SELECT EXISTS (SELECT 1 FROM teaching_assignments "
            + "WHERE class_id = :classId AND teacher_id = :teacherId)", nativeQuery = true)
    boolean teacherTeachesClass(@Param("teacherId") Long teacherId, @Param("classId") Long classId);
}
