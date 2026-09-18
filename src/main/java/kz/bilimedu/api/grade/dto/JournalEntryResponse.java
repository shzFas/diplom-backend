package kz.bilimedu.api.grade.dto;

import kz.bilimedu.api.grade.JournalRow;

/** Строка журнала. score равен null, если оценка не выставлена. */
public record JournalEntryResponse(String studentId, String fullName, String gradeId, Short score) {

    public static JournalEntryResponse from(JournalRow row) {
        return new JournalEntryResponse(
                String.valueOf(row.studentId()),
                row.fullName(),
                row.gradeId() == null ? null : String.valueOf(row.gradeId()),
                row.score());
    }
}
