package kz.bilimedu.api.calendar;

import jakarta.validation.Valid;
import kz.bilimedu.api.calendar.dto.AcademicYearResponse;
import kz.bilimedu.api.calendar.dto.CreateAcademicYearRequest;
import kz.bilimedu.api.calendar.dto.CreateTermRequest;
import kz.bilimedu.api.calendar.dto.TermResponse;
import kz.bilimedu.api.common.PageResponse;
import kz.bilimedu.api.common.Paging;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Календарь читают все роли: учителю он нужен, чтобы планировать, ученику —
 * чтобы понимать, за какую четверть он смотрит оценки. Меняет только завуч.
 */
@RestController
@RequestMapping("/api/v1")
public class CalendarController {

    private final CalendarService calendarService;

    public CalendarController(CalendarService calendarService) {
        this.calendarService = calendarService;
    }

    @GetMapping("/academic-years")
    public PageResponse<AcademicYearResponse> listYears(@RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "50") int size) {
        return calendarService.listYears(Paging.of(page, size));
    }

    @PostMapping("/academic-years")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public AcademicYearResponse createYear(@Valid @RequestBody CreateAcademicYearRequest request) {
        return calendarService.createYear(request);
    }

    @GetMapping("/terms")
    public PageResponse<TermResponse> listTerms(@RequestParam(required = false) Short academicYearId,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "50") int size) {
        return calendarService.listTerms(academicYearId, Paging.of(page, size));
    }

    @PostMapping("/terms")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public TermResponse createTerm(@Valid @RequestBody CreateTermRequest request) {
        return calendarService.createTerm(request);
    }

    @PostMapping("/terms/{id}/close")
    @PreAuthorize("hasRole('ADMIN')")
    public TermResponse close(@PathVariable Short id) {
        return calendarService.close(id);
    }

    @PostMapping("/terms/{id}/reopen")
    @PreAuthorize("hasRole('ADMIN')")
    public TermResponse reopen(@PathVariable Short id) {
        return calendarService.reopen(id);
    }
}
