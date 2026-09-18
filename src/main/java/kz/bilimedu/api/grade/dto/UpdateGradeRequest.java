package kz.bilimedu.api.grade.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Причина обязательна: правка оценки попадает в аудит и должна быть объяснима. */
public record UpdateGradeRequest(

        @NotNull @Min(0) Short score,

        @NotBlank @Size(max = 500) String reason) {
}
