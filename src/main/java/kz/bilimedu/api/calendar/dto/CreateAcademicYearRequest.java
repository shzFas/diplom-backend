package kz.bilimedu.api.calendar.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CreateAcademicYearRequest(

        @NotBlank @Size(max = 50) String name,

        @NotNull LocalDate startsOn,

        @NotNull LocalDate endsOn) {
}
