package kz.bilimedu.api.enrollment.dto;

import java.time.LocalDate;
import kz.bilimedu.api.enrollment.EnrollmentRow;

public record EnrollmentResponse(String id, String studentId, String classId, String className,
                                 String academicYearId, LocalDate fromDate, LocalDate toDate,
                                 boolean active) {

    public static EnrollmentResponse from(EnrollmentRow row) {
        return new EnrollmentResponse(
                String.valueOf(row.id()),
                String.valueOf(row.studentId()),
                String.valueOf(row.classId()),
                row.className(),
                String.valueOf(row.academicYearId()),
                row.fromDate(),
                row.toDate(),
                row.toDate() == null);
    }
}
