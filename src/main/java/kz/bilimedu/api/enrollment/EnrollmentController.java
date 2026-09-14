package kz.bilimedu.api.enrollment;

import jakarta.validation.Valid;
import java.time.LocalDate;
import kz.bilimedu.api.common.PageResponse;
import kz.bilimedu.api.common.Paging;
import kz.bilimedu.api.enrollment.dto.CreateEnrollmentRequest;
import kz.bilimedu.api.enrollment.dto.EnrollmentResponse;
import kz.bilimedu.api.enrollment.dto.RosterEntryResponse;
import kz.bilimedu.api.enrollment.dto.TransferRequest;
import kz.bilimedu.api.security.AuthenticatedUser;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Заменяет PUT /student/changeClass/:id — ручку, которая перезаписывала
 * Student.classId и тем самым уничтожала историю переводов.
 */
@RestController
@RequestMapping("/api/v1")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    public EnrollmentController(EnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    @PostMapping("/enrollments")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public EnrollmentResponse enroll(@Valid @RequestBody CreateEnrollmentRequest request) {
        return enrollmentService.enroll(request);
    }

    @PostMapping("/enrollments/{id}/transfer")
    @PreAuthorize("hasRole('ADMIN')")
    public EnrollmentResponse transfer(@PathVariable Long id,
                                       @Valid @RequestBody TransferRequest request) {
        return enrollmentService.transfer(id, request);
    }

    /** {@code ?on=} по умолчанию — сегодня. */
    @GetMapping("/classes/{id}/roster")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public PageResponse<RosterEntryResponse> roster(
            @AuthenticationPrincipal AuthenticatedUser viewer,
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate on,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return enrollmentService.roster(viewer, id, on, Paging.of(page, size));
    }

    @GetMapping("/students/{id}/enrollments")
    public PageResponse<EnrollmentResponse> history(@AuthenticationPrincipal AuthenticatedUser viewer,
                                                    @PathVariable Long id,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "50") int size) {
        return enrollmentService.history(viewer, id, Paging.of(page, size));
    }
}
