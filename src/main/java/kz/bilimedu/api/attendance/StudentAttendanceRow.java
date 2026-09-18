package kz.bilimedu.api.attendance;

import java.time.LocalDate;

/** Отметка вместе с уроком и предметом — чтобы сводка читалась одним запросом. */
public record StudentAttendanceRow(Long lessonId, String lessonTitle, LocalDate lessonDate,
                                   AttendanceStatus status, Long subjectId, String subjectName,
                                   Short termId) {
}
