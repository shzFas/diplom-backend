package kz.bilimedu.api.auth;

import static org.assertj.core.api.Assertions.assertThat;

import kz.bilimedu.api.auth.dto.ChangePasswordRequest;
import kz.bilimedu.api.auth.dto.LoginRequest;
import kz.bilimedu.api.auth.dto.RefreshRequest;
import kz.bilimedu.api.auth.dto.TokenPairResponse;
import kz.bilimedu.api.support.AbstractIntegrationTest;
import kz.bilimedu.api.user.Role;
import kz.bilimedu.api.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.client.EntityExchangeResult;

class AuthFlowIntegrationTest extends AbstractIntegrationTest {

    private EntityExchangeResult<ErrorEnvelope> loginExpectingError(String email, String password) {
        return client.post().uri("/api/v1/auth/login")
                .body(new LoginRequest(email, password))
                .exchange()
                .returnResult(ErrorEnvelope.class);
    }

    private EntityExchangeResult<ErrorEnvelope> refreshExpectingError(String refreshToken) {
        return client.post().uri("/api/v1/auth/refresh")
                .body(new RefreshRequest(refreshToken))
                .exchange()
                .returnResult(ErrorEnvelope.class);
    }

    @Test
    @DisplayName("вход выдаёт пару токенов и профиль пользователя")
    void loginIssuesTokenPair() {
        createUser("teacher@school.kz", Role.TEACHER);

        TokenPairResponse response = login("teacher@school.kz");

        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.refreshToken()).isNotBlank();
        assertThat(response.user().email()).isEqualTo("teacher@school.kz");
        assertThat(response.user().role()).isEqualTo(Role.TEACHER);
    }

    @Test
    @DisplayName("email нечувствителен к регистру — колонка citext")
    void loginIsCaseInsensitiveOnEmail() {
        createUser("teacher@school.kz", Role.TEACHER);

        TokenPairResponse response = client.post().uri("/api/v1/auth/login")
                .body(new LoginRequest("TEACHER@School.KZ", PASSWORD))
                .exchange()
                .returnResult(TokenPairResponse.class)
                .getResponseBody();

        assertThat(response.accessToken()).isNotBlank();
    }

    @Test
    @DisplayName("неверный пароль — 401 INVALID_CREDENTIALS")
    void wrongPasswordRejected() {
        createUser("teacher@school.kz", Role.TEACHER);

        EntityExchangeResult<ErrorEnvelope> result = loginExpectingError("teacher@school.kz", "wrong");

        assertThat(result.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(result.getResponseBody().error().code()).isEqualTo("INVALID_CREDENTIALS");
    }

    @Test
    @DisplayName("неизвестный email отвечает тем же кодом, что и неверный пароль")
    void unknownEmailIsIndistinguishable() {
        EntityExchangeResult<ErrorEnvelope> result = loginExpectingError("nobody@school.kz", PASSWORD);

        assertThat(result.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(result.getResponseBody().error().code()).isEqualTo("INVALID_CREDENTIALS");
    }

    @Test
    @DisplayName("деактивированный пользователь не входит")
    void deactivatedUserCannotLogin() {
        User user = createUser("fired@school.kz", Role.TEACHER);
        user.deactivate();
        users.save(user);

        EntityExchangeResult<ErrorEnvelope> result = loginExpectingError("fired@school.kz", PASSWORD);

        assertThat(result.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(result.getResponseBody().error().code()).isEqualTo("ACCOUNT_DEACTIVATED");
    }

    @Test
    @DisplayName("refresh ротирует токен: предъявленный отзывается")
    void refreshRotatesToken() {
        createUser("teacher@school.kz", Role.TEACHER);
        TokenPairResponse first = login("teacher@school.kz");

        TokenPairResponse second = client.post().uri("/api/v1/auth/refresh")
                .body(new RefreshRequest(first.refreshToken()))
                .exchange()
                .returnResult(TokenPairResponse.class)
                .getResponseBody();
        assertThat(second.refreshToken()).isNotEqualTo(first.refreshToken());

        EntityExchangeResult<ErrorEnvelope> reuse = refreshExpectingError(first.refreshToken());
        assertThat(reuse.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(reuse.getResponseBody().error().code()).isEqualTo("REFRESH_TOKEN_INVALID");
    }

    @Test
    @DisplayName("logout отзывает предъявленный refresh-токен")
    void logoutRevokesRefreshToken() {
        createUser("teacher@school.kz", Role.TEACHER);
        TokenPairResponse tokens = login("teacher@school.kz");

        EntityExchangeResult<Void> logout = authorized(tokens.accessToken())
                .post().uri("/api/v1/auth/logout")
                .body(new RefreshRequest(tokens.refreshToken()))
                .exchange()
                .returnResult(Void.class);
        assertThat(logout.getStatus()).isEqualTo(HttpStatus.NO_CONTENT);

        assertThat(refreshExpectingError(tokens.refreshToken()).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    /** Приёмочный критерий фазы 02: после смены пароля прежний refresh-токен получает 401. */
    @Test
    @DisplayName("смена пароля отзывает все refresh-токены пользователя")
    void passwordChangeRevokesEveryRefreshToken() {
        createUser("teacher@school.kz", Role.TEACHER);
        TokenPairResponse firstSession = login("teacher@school.kz");
        TokenPairResponse secondSession = login("teacher@school.kz");

        EntityExchangeResult<Void> change = authorized(firstSession.accessToken())
                .post().uri("/api/v1/me/password")
                .body(new ChangePasswordRequest(PASSWORD, "new-password-long-enough"))
                .exchange()
                .returnResult(Void.class);
        assertThat(change.getStatus()).isEqualTo(HttpStatus.NO_CONTENT);

        assertThat(refreshExpectingError(firstSession.refreshToken()).getStatus())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(refreshExpectingError(secondSession.refreshToken()).getStatus())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("после смены пароля вход идёт по новому паролю")
    void passwordChangeTakesEffect() {
        createUser("teacher@school.kz", Role.TEACHER);
        TokenPairResponse session = login("teacher@school.kz");

        authorized(session.accessToken())
                .post().uri("/api/v1/me/password")
                .body(new ChangePasswordRequest(PASSWORD, "new-password-long-enough"))
                .exchange()
                .returnResult(Void.class);

        assertThat(loginExpectingError("teacher@school.kz", PASSWORD).getStatus())
                .isEqualTo(HttpStatus.UNAUTHORIZED);

        TokenPairResponse renewed = client.post().uri("/api/v1/auth/login")
                .body(new LoginRequest("teacher@school.kz", "new-password-long-enough"))
                .exchange()
                .returnResult(TokenPairResponse.class)
                .getResponseBody();
        assertThat(renewed.accessToken()).isNotBlank();
    }

    @Test
    @DisplayName("смена пароля с неверным текущим паролем не проходит")
    void passwordChangeRequiresCurrentPassword() {
        createUser("teacher@school.kz", Role.TEACHER);
        TokenPairResponse session = login("teacher@school.kz");

        EntityExchangeResult<ErrorEnvelope> result = authorized(session.accessToken())
                .post().uri("/api/v1/me/password")
                .body(new ChangePasswordRequest("not-the-password", "new-password-long-enough"))
                .exchange()
                .returnResult(ErrorEnvelope.class);

        assertThat(result.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(result.getResponseBody().error().code()).isEqualTo("INVALID_CREDENTIALS");
    }
}
