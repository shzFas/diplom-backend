package kz.bilimedu.api.enrollment;

import java.time.LocalDate;

/** Зачисление вместе с названием класса — чтобы история переводов читалась без второго запроса. */
public record EnrollmentRow(Long id, Long studentId, Long classId, String className,
                            Short academicYearId, LocalDate fromDate, LocalDate toDate) {
}
