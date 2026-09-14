package kz.bilimedu.api.lesson.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * PATCH: null означает «не трогать это поле».
 *
 * <p>Ни назначение, ни четверть, ни тип оценивания сменить нельзя.
 * Перенос урока в другое назначение оставил бы выставленные оценки
 * учеников чужого класса, а смена типа с SOR на SOCH обошла бы правило
 * «один СОЧ на четверть» для уже проведённой работы.
 */
public record UpdateLessonRequest(

        @Size(max = 200) String title,

        LocalDate lessonDate,

        @Min(1) @Max(100) Short maxScore) {
}
