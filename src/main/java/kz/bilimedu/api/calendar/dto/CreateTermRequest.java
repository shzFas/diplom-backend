package kz.bilimedu.api.calendar.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * Правило D1 — «не более четырёх четвертей в году» — стоит и в базе
 * (CHECK ordinal BETWEEN 1 AND 4). Здесь оно повторено только чтобы
 * отдать 400 с внятным полем вместо 409 от базы.
 */
public record CreateTermRequest(

        @NotNull Short academicYearId,

        @NotNull @Min(1) @Max(4) Short ordinal,

        @NotNull LocalDate startsOn,

        @NotNull LocalDate endsOn) {
}
