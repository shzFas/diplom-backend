package kz.bilimedu.api.lesson;

import jakarta.validation.Valid;
import kz.bilimedu.api.common.PageResponse;
import kz.bilimedu.api.common.Paging;
import kz.bilimedu.api.lesson.dto.CreateLessonRequest;
import kz.bilimedu.api.lesson.dto.LessonResponse;
import kz.bilimedu.api.lesson.dto.UpdateLessonRequest;
import kz.bilimedu.api.security.AuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Заменяет POST /ktp, GET /ktps и GET /ktp/period/:classId/:period/:predmetId —
 * последнюю вместе с фильтрами, которые лежали в сегментах пути.
 *
 * <p>Писать уроки может только TEACHER: по матрице прав завуч журнал читает,
 * но не ведёт. Это то же решение, что и с оценками, — иначе административная
 * роль обходила бы авторство и делала аудит бессмысленным.
 */
@RestController
@RequestMapping("/api/v1/lessons")
public class LessonController {

    private final LessonService lessonService;

    public LessonController(LessonService lessonService) {
        this.lessonService = lessonService;
    }

    @GetMapping
    public PageResponse<LessonResponse> list(@AuthenticationPrincipal AuthenticatedUser viewer,
                                             @RequestParam(required = false) Long assignmentId,
                                             @RequestParam(required = false) Short termId,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "50") int size) {
        return lessonService.list(viewer, assignmentId, termId, Paging.of(page, size));
    }

    @GetMapping("/{id}")
    public LessonResponse get(@AuthenticationPrincipal AuthenticatedUser viewer, @PathVariable Long id) {
        return lessonService.get(viewer, id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('TEACHER')")
    public LessonResponse create(@AuthenticationPrincipal AuthenticatedUser teacher,
                                 @Valid @RequestBody CreateLessonRequest request) {
        return lessonService.create(teacher, request);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public LessonResponse update(@AuthenticationPrincipal AuthenticatedUser teacher,
                                 @PathVariable Long id,
                                 @Valid @RequestBody UpdateLessonRequest request) {
        return lessonService.update(teacher, id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AuthenticatedUser teacher,
                                       @PathVariable Long id) {
        lessonService.delete(teacher, id);
        return ResponseEntity.noContent().build();
    }
}
