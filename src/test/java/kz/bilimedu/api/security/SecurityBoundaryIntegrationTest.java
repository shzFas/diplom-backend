package kz.bilimedu.api.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Map;
import kz.bilimedu.api.auth.dto.TokenPairResponse;
import kz.bilimedu.api.config.JwtProperties;
import kz.bilimedu.api.support.AbstractIntegrationTest;
import kz.bilimedu.api.user.Role;
import kz.bilimedu.api.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.client.EntityExchangeResult;

/**
 * Границы доступа. Отправная точка — версия 2023 года, где эти же запросы
 * отвечали 200 анонимному вызову.
 */
class SecurityBoundaryIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private JwtProperties jwtProperties;

    private EntityExchangeResult<ErrorEnvelope> meWithToken(String accessToken) {
        return authorized(accessToken).get().uri("/api/v1/me").exchange().returnResult(ErrorEnvelope.class);
    }

    /** Приёмочный критерий: запрос без токена получает 401 на непубличном эндпоинте. */
    @Test
    @DisplayName("без токена непубличный эндпоинт отвечает 401 TOKEN_MISSING")
    void missingTokenRejected() {
        EntityExchangeResult<ErrorEnvelope> result =
                client.get().uri("/api/v1/me").exchange().returnResult(ErrorEnvelope.class);

        assertThat(result.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(result.getResponseBody().error().code()).isEqualTo("TOKEN_MISSING");
    }

    @Test
    @DisplayName("повреждённый токен — 401 TOKEN_INVALID, а не 500")
    void malformedTokenRejected() {
        EntityExchangeResult<ErrorEnvelope> result = meWithToken("not.a.jwt");

        assertThat(result.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(result.getResponseBody().error().code()).isEqualTo("TOKEN_INVALID");
    }

    @Test
    @DisplayName("токен, подписанный чужим секретом, не принимается")
    void foreignSignatureRejected() {
        User user = createUser("teacher@school.kz", Role.TEACHER);
        JwtService forger = new JwtService(new JwtProperties(
                "a-completely-different-secret-key-32-chars", jwtProperties.issuer(),
                Duration.ofMinutes(15), Duration.ofDays(30)));

        EntityExchangeResult<ErrorEnvelope> result = meWithToken(forger.issueAccessToken(user));

        assertThat(result.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(result.getResponseBody().error().code()).isEqualTo("TOKEN_INVALID");
    }

    @Test
    @DisplayName("истёкший токен отличается от отсутствующего: TOKEN_EXPIRED")
    void expiredTokenReportedSeparately() {
        User user = createUser("teacher@school.kz", Role.TEACHER);
        JwtService expiredIssuer = new JwtService(new JwtProperties(
                jwtProperties.secret(), jwtProperties.issuer(),
                Duration.ofMinutes(-15), jwtProperties.refreshTtl()));

        EntityExchangeResult<ErrorEnvelope> result = meWithToken(expiredIssuer.issueAccessToken(user));

        assertThat(result.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(result.getResponseBody().error().code()).isEqualTo("TOKEN_EXPIRED");
    }

    /** Приёмочный критерий: passwordHash не встречается ни в одном ответе API. */
    @Test
    @DisplayName("ответ GET /me не содержит хеша пароля")
    void profileNeverLeaksPasswordHash() {
        createUser("teacher@school.kz", Role.TEACHER);
        TokenPairResponse tokens = login("teacher@school.kz");

        EntityExchangeResult<String> result = authorized(tokens.accessToken())
                .get().uri("/api/v1/me").exchange().returnResult(String.class);

        assertThat(result.getStatus()).isEqualTo(HttpStatus.OK);
        assertThat(result.getResponseBody())
                .doesNotContain("passwordHash")
                .doesNotContain("password_hash")
                .doesNotContain("$argon2");
    }

    @Test
    @DisplayName("id пользователя приходит строкой — bigint не помещается в double")
    void identifiersAreStrings() {
        createUser("teacher@school.kz", Role.TEACHER);
        TokenPairResponse tokens = login("teacher@school.kz");

        EntityExchangeResult<String> result = authorized(tokens.accessToken())
                .get().uri("/api/v1/me").exchange().returnResult(String.class);

        assertThat(result.getResponseBody()).containsPattern("\"id\"\\s*:\\s*\"\\d+\"");
    }

    @Test
    @DisplayName("публичные эндпоинты health и docs открыты без токена")
    void publicEndpointsAreReachableWithoutToken() {
        assertThat(client.get().uri("/api/v1/health").exchange().returnResult(String.class).getStatus())
                .isEqualTo(HttpStatus.OK);

        EntityExchangeResult<String> docs =
                client.get().uri("/api/v1/docs").exchange().returnResult(String.class);
        assertThat(docs.getStatus()).isEqualTo(HttpStatus.OK);
        assertThat(docs.getResponseBody()).contains("Контракт API v1");
    }

    @Test
    @DisplayName("регистрации самозаписью нет: POST /auth/register закрыт")
    void selfRegistrationIsGone() {
        EntityExchangeResult<String> result = client.post().uri("/api/v1/auth/register")
                .body(Map.of()).exchange().returnResult(String.class);

        assertThat(result.getStatus()).isIn(HttpStatus.UNAUTHORIZED, HttpStatus.NOT_FOUND,
                HttpStatus.METHOD_NOT_ALLOWED);
    }

    @Test
    @DisplayName("сообщение об ошибке локализуется по Accept-Language")
    void errorMessageFollowsAcceptLanguage() {
        EntityExchangeResult<ErrorEnvelope> result = client.get().uri("/api/v1/me")
                .header(HttpHeaders.ACCEPT_LANGUAGE, "kk")
                .exchange()
                .returnResult(ErrorEnvelope.class);

        assertThat(result.getResponseBody().error().code()).isEqualTo("TOKEN_MISSING");
        assertThat(result.getResponseBody().error().message()).isEqualTo("Access-токен қажет");
    }

    @Test
    @DisplayName("валидация тела запроса перечисляет поля в details")
    void validationFailureListsFields() {
        EntityExchangeResult<ErrorEnvelope> result = client.post().uri("/api/v1/auth/login")
                .body(Map.of("email", "not-an-email", "password", ""))
                .exchange()
                .returnResult(ErrorEnvelope.class);

        assertThat(result.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(result.getResponseBody().error().code()).isEqualTo("VALIDATION_FAILED");
        assertThat(result.getResponseBody().error().details()).isNotEmpty();
    }
}
