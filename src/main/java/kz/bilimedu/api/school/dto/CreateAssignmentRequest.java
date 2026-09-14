package kz.bilimedu.api.school.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Заменяет PUT /teacher/:id, которым в версии 2023 года в массив
 * User.permission[] дописывался произвольный элемент без всякой проверки.
 */
public record CreateAssignmentRequest(

        @NotNull Long classId,

        @NotNull Long subjectId,

        @NotNull Long teacherId) {
}
