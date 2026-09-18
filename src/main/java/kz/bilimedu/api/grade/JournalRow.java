package kz.bilimedu.api.grade;

/**
 * Строка журнала по уроку: ученик из состава класса на дату урока и его
 * оценка, если она выставлена. Оценки может не быть — это нормально,
 * посещаемость и оценка независимы (правило D10).
 */
public record JournalRow(Long studentId, String fullName, Long gradeId, Short score) {
}
