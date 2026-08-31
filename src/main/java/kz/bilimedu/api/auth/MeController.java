package kz.bilimedu.api.auth;

import jakarta.validation.Valid;
import kz.bilimedu.api.auth.dto.ChangePasswordRequest;
import kz.bilimedu.api.common.ApiException;
import kz.bilimedu.api.common.ErrorCode;
import kz.bilimedu.api.security.AuthenticatedUser;
import kz.bilimedu.api.user.UserRepository;
import kz.bilimedu.api.user.UserResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Ручки «про себя». Заменяют пары getMe/getStudentMe и changePassword,
 * которые в версии 2023 года были продублированы для двух коллекций.
 */
@RestController
@RequestMapping("/api/v1/me")
public class MeController {

    private final UserRepository users;
    private final AuthService authService;

    public MeController(UserRepository users, AuthService authService) {
        this.users = users;
        this.authService = authService;
    }

    @GetMapping
    public UserResponse me(@AuthenticationPrincipal AuthenticatedUser principal) {
        return users.findById(principal.id())
                .map(UserResponse::from)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
    }

    @PostMapping("/password")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal AuthenticatedUser principal,
                                               @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(principal.id(), request.current(), request.updated());
        return ResponseEntity.noContent().build();
    }
}
