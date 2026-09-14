package kz.bilimedu.api.enrollment.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * Заменяет PUT /student/changeClass/:id, который просто перезаписывал
 * Student.classId и тем самым переписывал прошлое ученика.
 *
 * <p>{@code fromDate} — первый день в новом классе. Прежнее зачисление
 * закрывается предыдущим днём, поэтому история остаётся непрерывной
 * и без пересечений.
 */
public record TransferRequest(

        @NotNull Long toClassId,

        @NotNull LocalDate fromDate) {
}
