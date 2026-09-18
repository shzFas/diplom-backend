package kz.bilimedu.api.attendance;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import kz.bilimedu.api.attendance.dto.AttendanceEntryRequest;
import kz.bilimedu.api.attendance.dto.AttendanceResponse;
import kz.bilimedu.api.attendance.dto.LessonAttendanceEntryResponse;
import kz.bilimedu.api.attendance.dto.StudentAttendanceResponse;
import kz.bilimedu.api.common.PageResponse;
import kz.bilimedu.api.common.Paging;
import kz.bilimedu.api.security.AuthenticatedUser;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Посещаемость. Заменяет булев markFalse, который не отличал болезнь
 * от прогула — а школа отчитывается именно этим различием.
 */
@RestController
@RequestMapping("/api/v1")
public class AttendanceController {

    private static final int MAX_BULK_ENTRIES = 200;

    private final AttendanceService attendanceService;

    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    /**
     * Дополнение к контракту: он описывал только запись. Без чтения ведомость
     * непригодна — учитель не видел бы, кого уже отметил.
     */
    @GetMapping("/lessons/{id}/attendance")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public PageResponse<LessonAttendanceEntryResponse> lessonSheet(
            @AuthenticationPrincipal AuthenticatedUser viewer,
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return attendanceService.lessonSheet(viewer, id, Paging.of(page, size));
    }

    @PutMapping("/lessons/{id}/attendance")
    @PreAuthorize("hasRole('TEACHER')")
    public List<AttendanceResponse> putAttendance(
            @AuthenticationPrincipal AuthenticatedUser teacher,
            @PathVariable Long id,
            @RequestBody @NotEmpty @Size(max = MAX_BULK_ENTRIES)
            List<@Valid AttendanceEntryRequest> entries) {
        return attendanceService.putAttendance(teacher, id, entries);
    }

    @GetMapping("/students/{id}/attendance")
    public PageResponse<StudentAttendanceResponse> studentAttendance(
            @AuthenticationPrincipal AuthenticatedUser viewer,
            @PathVariable Long id,
            @RequestParam(required = false) Short termId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return attendanceService.studentSummary(viewer, id, termId, Paging.of(page, size));
    }
}
