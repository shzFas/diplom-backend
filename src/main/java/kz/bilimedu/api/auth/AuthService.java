package kz.bilimedu.api.auth;

import java.time.Instant;
import kz.bilimedu.api.auth.dto.TokenPairResponse;
import kz.bilimedu.api.common.ApiException;
import kz.bilimedu.api.common.ErrorCode;
import kz.bilimedu.api.security.JwtService;
import kz.bilimedu.api.user.User;
import kz.bilimedu.api.user.UserRepository;
import kz.bilimedu.api.user.UserResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Вход, обновление и отзыв токенов.
 *
 * <p>Refresh-токен ротируется: каждое обновление отзывает предъявленный токен
 * и выдаёт новый. Повторное использование уже потраченного токена не проходит —
 * это и есть признак утечки.
 */
@Service
public class AuthService {

    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository users,
                       RefreshTokenRepository refreshTokens,
                       JwtService jwtService,
                       PasswordEncoder passwordEncoder) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public TokenPairResponse login(String email, String rawPassword) {
        User user = users.findByEmail(email).orElse(null);

        // Пароль проверяется даже для несуществующего пользователя: иначе
        // разница во времени ответа выдаёт, какие email заведены в системе.
        String storedHash = user != null ? user.getPasswordHash() : "";
        boolean passwordMatches = passwordEncoder.matches(rawPassword, storedHash);

        if (user == null || !passwordMatches) {
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS);
        }
        if (!user.isActive()) {
            throw new ApiException(ErrorCode.ACCOUNT_DEACTIVATED);
        }

        return issuePair(user);
    }

    @Transactional
    public TokenPairResponse refresh(String rawRefreshToken) {
        RefreshToken stored = refreshTokens.findByTokenHash(jwtService.hashRefreshToken(rawRefreshToken))
                .orElseThrow(() -> new ApiException(ErrorCode.REFRESH_TOKEN_INVALID));

        if (!stored.isUsable(Instant.now())) {
            throw new ApiException(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        User user = users.findById(stored.getUserId())
                .orElseThrow(() -> new ApiException(ErrorCode.REFRESH_TOKEN_INVALID));
        if (!user.isActive()) {
            throw new ApiException(ErrorCode.ACCOUNT_DEACTIVATED);
        }

        stored.revoke();
        return issuePair(user);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokens.findByTokenHash(jwtService.hashRefreshToken(rawRefreshToken))
                .ifPresent(RefreshToken::revoke);
    }

    /** Смена пароля отзывает все refresh-токены пользователя, а не только текущий. */
    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = users.findById(userId).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS);
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        users.save(user);
        refreshTokens.revokeAllForUser(userId, Instant.now());
    }

    private TokenPairResponse issuePair(User user) {
        String rawRefreshToken = jwtService.generateRefreshToken();
        refreshTokens.save(new RefreshToken(
                user.getId(),
                jwtService.hashRefreshToken(rawRefreshToken),
                jwtService.refreshTokenExpiry()));

        return new TokenPairResponse(jwtService.issueAccessToken(user), rawRefreshToken, UserResponse.from(user));
    }
}
