package kz.bilimedu.api.enrollment;

import java.time.LocalDate;

/** Строка состава класса: ученик плюс даты его зачисления. */
public record RosterRow(Long studentId, String fullName, String email,
                        LocalDate fromDate, LocalDate toDate) {
}
