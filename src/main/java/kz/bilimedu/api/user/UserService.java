package kz.bilimedu.api.user;

import java.time.Instant;
import kz.bilimedu.api.auth.RefreshTokenRepository;
import kz.bilimedu.api.common.ApiException;
import kz.bilimedu.api.common.ErrorCode;
import kz.bilimedu.api.common.PageResponse;
import kz.bilimedu.api.security.AuthenticatedUser;
import kz.bilimedu.api.user.dto.CreateUserRequest;
import kz.bilimedu.api.user.dto.UpdateUserRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Справочник пользователей. Роль решает, что можно делать, а предикаты
 * владения — с кем именно: в версии 2023 года GET /students и
 * DELETE /student/:id отвечали кому угодно, включая анонимный вызов.
 */
@Service
public class UserService {

    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository users,
                       RefreshTokenRepository refreshTokens,
                       PasswordEncoder passwordEncoder) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> list(AuthenticatedUser viewer,
                                           Role role,
                                           Long classId,
                                           boolean includeDeactivated,
                                           Pageable pageable) {
        Page<User> page = users.findVisible(
                viewer.id(),
                viewer.role().name(),
                role == null ? null : role.name(),
                classId,
                includeDeactivated,
                pageable);
        return PageResponse.of(page.map(UserResponse::from));
    }

    @Transactional(readOnly = true)
    public UserResponse get(AuthenticatedUser viewer, Long id) {
        User target = users.findById(id).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        if (!canRead(viewer, target)) {
            throw new ApiException(ErrorCode.NOT_OWNER);
        }
        return UserResponse.from(target);
    }

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        if (users.emailTaken(request.email())) {
            throw new ApiException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        User created = users.save(new User(
                request.fullName().strip(),
                request.email().strip(),
                passwordEncoder.encode(request.password()),
                request.role()));
        return UserResponse.from(created);
    }

    @Transactional
    public UserResponse update(Long id, UpdateUserRequest request) {
        User target = users.findById(id).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));

        if (request.fullName() != null) {
            target.rename(request.fullName().strip());
        }
        if (request.email() != null) {
            String email = request.email().strip();
            if (!email.equalsIgnoreCase(target.getEmail()) && users.emailTaken(email)) {
                throw new ApiException(ErrorCode.EMAIL_ALREADY_EXISTS);
            }
            target.changeEmail(email);
        }
        if (request.avatarUrl() != null) {
            target.setAvatarUrl(request.avatarUrl().isBlank() ? null : request.avatarUrl().strip());
        }
        return UserResponse.from(users.save(target));
    }

    /**
     * DELETE — это деактивация. Учитель остаётся автором выставленных оценок
     * (grades.graded_by с ON DELETE RESTRICT), поэтому физическое удаление
     * либо не прошло бы, либо унесло бы оценки за собой.
     */
    @Transactional
    public void deactivate(AuthenticatedUser actor, Long id) {
        if (actor.id().equals(id)) {
            throw new ApiException(ErrorCode.CANNOT_DEACTIVATE_SELF);
        }
        User target = users.findById(id).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));

        if (target.isActive()) {
            target.deactivate();
            users.save(target);
            // Деактивация должна выкинуть пользователя немедленно, иначе он
            // проработает по живому refresh-токену ещё тридцать дней.
            refreshTokens.revokeAllForUser(target.getId(), Instant.now());
        }
    }

    private boolean canRead(AuthenticatedUser viewer, User target) {
        if (viewer.role() == Role.ADMIN || viewer.id().equals(target.getId())) {
            return true;
        }
        if (viewer.role() != Role.TEACHER) {
            return false;
        }
        return target.getRole() == Role.TEACHER
                || users.teacherTeachesStudent(viewer.id(), target.getId());
    }
}
