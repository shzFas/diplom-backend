package kz.bilimedu.api.attendance.dto;

import kz.bilimedu.api.attendance.AttendanceStatus;
import kz.bilimedu.api.attendance.LessonAttendanceRow;

/** status равен null, если ученик ещё не отмечен. */
public record LessonAttendanceEntryResponse(String studentId, String fullName,
                                            AttendanceStatus status) {

    public static LessonAttendanceEntryResponse from(LessonAttendanceRow row) {
        return new LessonAttendanceEntryResponse(
                String.valueOf(row.studentId()), row.fullName(), row.status());
    }
}
