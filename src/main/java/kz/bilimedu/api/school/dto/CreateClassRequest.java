package kz.bilimedu.api.school.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateClassRequest(

        @NotNull Short academicYearId,

        @NotBlank @Size(max = 30) String name) {
}
