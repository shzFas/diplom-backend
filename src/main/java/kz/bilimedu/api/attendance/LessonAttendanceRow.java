package kz.bilimedu.api.attendance;

/**
 * Строка ведомости по уроку: ученик из состава класса на дату урока и его
 * статус, если он отмечен. status может быть null — неотмеченный ученик
 * не то же самое, что присутствовавший.
 */
public record LessonAttendanceRow(Long studentId, String fullName, AttendanceStatus status) {
}
