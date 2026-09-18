package kz.bilimedu.api.grade.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Одна строка в списке выставления оценок. Верхняя граница балла не
 * проверяется здесь: она лежит на уроке, поэтому её держит триггер
 * grade_within_lesson_max и возвращает GRADE_EXCEEDS_MAX.
 */
public record GradeEntryRequest(

        @NotNull Long studentId,

        @NotNull @Min(0) Short score) {
}
