package kz.bilimedu.api.school;

import jakarta.validation.Valid;
import kz.bilimedu.api.common.PageResponse;
import kz.bilimedu.api.common.Paging;
import kz.bilimedu.api.school.dto.AssignmentResponse;
import kz.bilimedu.api.school.dto.ClassResponse;
import kz.bilimedu.api.school.dto.CreateAssignmentRequest;
import kz.bilimedu.api.school.dto.CreateClassRequest;
import kz.bilimedu.api.school.dto.CreateSubjectRequest;
import kz.bilimedu.api.school.dto.SubjectResponse;
import kz.bilimedu.api.security.AuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Заменяет GET /predmet, POST /subject, GET /predmet/class/:id,
 * PUT /teacher/:id и DELETE /teacher/:id/:permissionId — пять ручек,
 * из которых ни одна не проверяла прав.
 */
@RestController
@RequestMapping("/api/v1")
public class SchoolController {

    private final SchoolService schoolService;

    public SchoolController(SchoolService schoolService) {
        this.schoolService = schoolService;
    }

    @GetMapping("/classes")
    public PageResponse<ClassResponse> listClasses(@AuthenticationPrincipal AuthenticatedUser viewer,
                                                   @RequestParam(required = false) Short academicYearId,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "50") int size) {
        return schoolService.listClasses(viewer, academicYearId, Paging.of(page, size));
    }

    @PostMapping("/classes")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ClassResponse createClass(@Valid @RequestBody CreateClassRequest request) {
        return schoolService.createClass(request);
    }

    @GetMapping("/subjects")
    public PageResponse<SubjectResponse> listSubjects(@AuthenticationPrincipal AuthenticatedUser viewer,
                                                      @RequestParam(required = false) Long classId,
                                                      @RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "50") int size) {
        return schoolService.listSubjects(viewer, classId, Paging.of(page, size));
    }

    @PostMapping("/subjects")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public SubjectResponse createSubject(@Valid @RequestBody CreateSubjectRequest request) {
        return schoolService.createSubject(request);
    }

    /** Ученику назначения не показываются вовсе (permissions.md). */
    @GetMapping("/teaching-assignments")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public PageResponse<AssignmentResponse> listAssignments(
            @AuthenticationPrincipal AuthenticatedUser viewer,
            @RequestParam(required = false) Long teacherId,
            @RequestParam(required = false) Long classId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return schoolService.listAssignments(viewer, teacherId, classId, Paging.of(page, size));
    }

    @PostMapping("/teaching-assignments")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public AssignmentResponse createAssignment(@Valid @RequestBody CreateAssignmentRequest request) {
        return schoolService.createAssignment(request);
    }

    @DeleteMapping("/teaching-assignments/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteAssignment(@PathVariable Long id) {
        schoolService.deleteAssignment(id);
        return ResponseEntity.noContent().build();
    }
}
