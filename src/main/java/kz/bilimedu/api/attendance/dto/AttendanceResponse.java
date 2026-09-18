package kz.bilimedu.api.attendance.dto;

import kz.bilimedu.api.attendance.Attendance;
import kz.bilimedu.api.attendance.AttendanceStatus;

public record AttendanceResponse(String lessonId, String studentId, AttendanceStatus status,
                                 String notedBy) {

    public static AttendanceResponse from(Attendance attendance) {
        return new AttendanceResponse(
                String.valueOf(attendance.getLessonId()),
                String.valueOf(attendance.getStudentId()),
                attendance.getStatus(),
                String.valueOf(attendance.getNotedBy()));
    }
}
