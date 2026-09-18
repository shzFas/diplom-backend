package kz.bilimedu.api.grade.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Причина обязательна. В версии 2023 года deleteOne удалял оценку бесследно
 * и без объяснений.
 */
public record DeleteGradeRequest(@NotBlank @Size(max = 500) String reason) {
}
