package kz.bilimedu.api.attendance;

import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AttendanceRepository extends JpaRepository<Attendance, AttendanceId> {

    Optional<Attendance> findByLessonIdAndStudentId(Long lessonId, Long studentId);

    /**
     * Ведомость по уроку: состав класса на дату урока плюс отметки.
     * Состав берётся из enrollments по дате урока, как и в журнале оценок.
     */
    @Query("""
            SELECT new kz.bilimedu.api.attendance.LessonAttendanceRow(u.id, u.fullName, t.status)
            FROM Enrollment e
            JOIN User u ON u.id = e.studentId
            LEFT JOIN Attendance t ON t.studentId = u.id AND t.lessonId = :lessonId
            WHERE e.classId = :classId
              AND e.fromDate <= :lessonDate
              AND (e.toDate IS NULL OR e.toDate >= :lessonDate)
            ORDER BY u.fullName, u.id
            """)
    Page<LessonAttendanceRow> lessonSheet(@Param("lessonId") Long lessonId,
                                          @Param("classId") Long classId,
                                          @Param("lessonDate") LocalDate lessonDate,
                                          Pageable pageable);

    /** Сводка по ученику: отметки с уроком, предметом и четвертью. */
    @Query("""
            SELECT new kz.bilimedu.api.attendance.StudentAttendanceRow(
                   l.id, l.title, l.lessonDate, t.status, s.id, s.name, l.termId)
            FROM Attendance t
            JOIN Lesson l ON l.id = t.lessonId
            JOIN TeachingAssignment a ON a.id = l.assignmentId
            JOIN Subject s ON s.id = a.subjectId
            WHERE t.studentId = :studentId
              AND (:termId IS NULL OR l.termId = :termId)
            ORDER BY l.lessonDate DESC, l.id DESC
            """)
    Page<StudentAttendanceRow> studentSummary(@Param("studentId") Long studentId,
                                              @Param("termId") Short termId,
                                              Pageable pageable);
}
