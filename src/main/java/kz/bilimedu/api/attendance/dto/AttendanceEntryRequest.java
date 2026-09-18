package kz.bilimedu.api.attendance.dto;

import jakarta.validation.constraints.NotNull;
import kz.bilimedu.api.attendance.AttendanceStatus;

public record AttendanceEntryRequest(

        @NotNull Long studentId,

        @NotNull AttendanceStatus status) {
}
