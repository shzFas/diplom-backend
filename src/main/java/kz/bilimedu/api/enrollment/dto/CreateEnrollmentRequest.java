package kz.bilimedu.api.enrollment.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CreateEnrollmentRequest(

        @NotNull Long studentId,

        @NotNull Long classId,

        @NotNull LocalDate fromDate) {
}
