package kz.bilimedu.api.calendar.dto;

import java.time.Instant;
import java.time.LocalDate;
import kz.bilimedu.api.calendar.Term;

public record TermResponse(String id, String academicYearId, short ordinal,
                           LocalDate startsOn, LocalDate endsOn,
                           boolean closed, Instant closedAt) {

    public static TermResponse from(Term term) {
        return new TermResponse(
                String.valueOf(term.getId()),
                String.valueOf(term.getAcademicYearId()),
                term.getOrdinal(),
                term.getStartsOn(),
                term.getEndsOn(),
                term.isClosed(),
                term.getClosedAt());
    }
}
