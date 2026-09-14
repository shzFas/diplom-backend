package kz.bilimedu.api.lesson.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import kz.bilimedu.api.lesson.AssessmentKind;

/**
 * Заменяет POST /ktp. Предмет, класс и учитель не передаются: они выводятся
 * из назначения. В версии 2023 года их приходилось передавать строками,
 * и ничто не мешало прислать несогласованный набор.
 */
public record CreateLessonRequest(

        @NotNull Long assignmentId,

        @NotNull Short termId,

        @NotBlank @Size(max = 200) String title,

        @NotNull LocalDate lessonDate,

        @NotNull AssessmentKind kind,

        @NotNull @Min(1) @Max(100) Short maxScore) {
}
