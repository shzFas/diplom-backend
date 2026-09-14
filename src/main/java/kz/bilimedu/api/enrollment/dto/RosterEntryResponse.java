package kz.bilimedu.api.enrollment.dto;

import java.time.LocalDate;
import kz.bilimedu.api.enrollment.RosterRow;

public record RosterEntryResponse(String studentId, String fullName, String email,
                                  LocalDate fromDate, LocalDate toDate) {

    public static RosterEntryResponse from(RosterRow row) {
        return new RosterEntryResponse(
                String.valueOf(row.studentId()),
                row.fullName(),
                row.email(),
                row.fromDate(),
                row.toDate());
    }
}
