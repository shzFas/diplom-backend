package kz.bilimedu.api.school.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSubjectRequest(@NotBlank @Size(max = 100) String name) {
}
