package kz.bilimedu.api.user;

import jakarta.validation.Valid;
import kz.bilimedu.api.common.PageResponse;
import kz.bilimedu.api.common.Paging;
import kz.bilimedu.api.security.AuthenticatedUser;
import kz.bilimedu.api.user.dto.CreateUserRequest;
import kz.bilimedu.api.user.dto.UpdateUserRequest;
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
 * Заменяет GET /teacher, GET /students, PUT /teacher/:id и
 * DELETE /student/:id — четыре ручки без единой проверки прав.
 */
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public PageResponse<UserResponse> list(@AuthenticationPrincipal AuthenticatedUser viewer,
                                           @RequestParam(required = false) Role role,
                                           @RequestParam(required = false) Long classId,
                                           @RequestParam(defaultValue = "false") boolean includeDeactivated,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "50") int size) {
        return userService.list(viewer, role, classId, includeDeactivated, Paging.of(page, size));
    }

    @GetMapping("/{id}")
    public UserResponse get(@AuthenticationPrincipal AuthenticatedUser viewer, @PathVariable Long id) {
        return userService.get(viewer, id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse create(@Valid @RequestBody CreateUserRequest request) {
        return userService.create(request);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        return userService.update(id, request);
    }

    /** Деактивация, а не удаление. */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivate(@AuthenticationPrincipal AuthenticatedUser actor,
                                           @PathVariable Long id) {
        userService.deactivate(actor, id);
        return ResponseEntity.noContent().build();
    }
}
