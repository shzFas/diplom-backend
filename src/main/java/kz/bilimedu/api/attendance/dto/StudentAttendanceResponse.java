package kz.bilimedu.api.attendance.dto;

import java.time.LocalDate;
import kz.bilimedu.api.attendance.AttendanceStatus;
import kz.bilimedu.api.attendance.StudentAttendanceRow;

public record StudentAttendanceResponse(String lessonId, String lessonTitle, LocalDate lessonDate,
                                        AttendanceStatus status, String subjectId,
                                        String subjectName, String termId) {

    public static StudentAttendanceResponse from(StudentAttendanceRow row) {
        return new StudentAttendanceResponse(
                String.valueOf(row.lessonId()),
                row.lessonTitle(),
                row.lessonDate(),
                row.status(),
                String.valueOf(row.subjectId()),
                row.subjectName(),
                String.valueOf(row.termId()));
    }
}
