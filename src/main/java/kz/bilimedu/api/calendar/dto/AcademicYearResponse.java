package kz.bilimedu.api.calendar.dto;

import java.time.LocalDate;
import kz.bilimedu.api.calendar.AcademicYear;

public record AcademicYearResponse(String id, String name, LocalDate startsOn, LocalDate endsOn) {

    public static AcademicYearResponse from(AcademicYear year) {
        return new AcademicYearResponse(
                String.valueOf(year.getId()), year.getName(), year.getStartsOn(), year.getEndsOn());
    }
}
