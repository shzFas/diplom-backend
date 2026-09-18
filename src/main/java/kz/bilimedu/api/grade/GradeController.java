package kz.bilimedu.api.grade;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import kz.bilimedu.api.common.PageResponse;
import kz.bilimedu.api.common.Paging;
import kz.bilimedu.api.grade.dto.DeleteGradeRequest;
import kz.bilimedu.api.grade.dto.GradeAuditResponse;
import kz.bilimedu.api.grade.dto.GradeEntryRequest;
import kz.bilimedu.api.grade.dto.GradeResponse;
import kz.bilimedu.api.grade.dto.JournalEntryResponse;
import kz.bilimedu.api.grade.dto.StudentGradeResponse;
import kz.bilimedu.api.grade.dto.UpdateGradeRequest;
import kz.bilimedu.api.security.AuthenticatedUser;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Заменяет POST /marks, GET /marksall, DELETE /marks/:studentId/:ktpId и
 * GET /marks/final/... — ручки без единой проверки прав, из которых
 * GET /marksall отдавал все оценки школы одним массивом.
 *
 * <p>Писать оценки может только TEACHER. ADMIN намеренно не имеет права
 * записи: правка оценок — исключительно учительское действие, и обход
 * этого правила административной ролью сделал бы аудит бессмысленным.
 */
@RestController
@RequestMapping("/api/v1")
public class GradeController {

    /** Класс, а не школа: верхняя граница тела не даёт прислать журнал на сто тысяч строк. */
    private static final int MAX_BULK_ENTRIES = 200;

    private final GradeService gradeService;

    public GradeController(GradeService gradeService) {
        this.gradeService = gradeService;
    }

    @GetMapping("/lessons/{id}/grades")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public PageResponse<JournalEntryResponse> journal(@AuthenticationPrincipal AuthenticatedUser viewer,
                                                      @PathVariable Long id,
                                                      @RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "50") int size) {
        return gradeService.journal(viewer, id, Paging.of(page, size));
    }

    @PutMapping("/lessons/{id}/grades")
    @PreAuthorize("hasRole('TEACHER')")
    public List<GradeResponse> putGrades(@AuthenticationPrincipal AuthenticatedUser teacher,
                                         @PathVariable Long id,
                                         @RequestBody @NotEmpty @Size(max = MAX_BULK_ENTRIES)
                                         List<@Valid GradeEntryRequest> entries) {
        return gradeService.putGrades(teacher, id, entries);
    }

    @PatchMapping("/grades/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public GradeResponse update(@AuthenticationPrincipal AuthenticatedUser teacher,
                                @PathVariable Long id,
                                @Valid @RequestBody UpdateGradeRequest request) {
        return gradeService.update(teacher, id, request);
    }

    @DeleteMapping("/grades/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AuthenticatedUser teacher,
                                       @PathVariable Long id,
                                       @Valid @RequestBody DeleteGradeRequest request) {
        gradeService.delete(teacher, id, request);
        return ResponseEntity.noContent().build();
    }

    /** Ученику аудит не показывается вовсе (permissions.md). */
    @GetMapping("/grades/{id}/audit")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public PageResponse<GradeAuditResponse> audit(@AuthenticationPrincipal AuthenticatedUser viewer,
                                                  @PathVariable Long id,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "50") int size) {
        return gradeService.audit(viewer, id, Paging.of(page, size));
    }

    @GetMapping("/students/{id}/grades")
    public PageResponse<StudentGradeResponse> studentGrades(
            @AuthenticationPrincipal AuthenticatedUser viewer,
            @PathVariable Long id,
            @RequestParam(required = false) Short termId,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return gradeService.studentDiary(viewer, id, termId, subjectId, Paging.of(page, size));
    }
}
