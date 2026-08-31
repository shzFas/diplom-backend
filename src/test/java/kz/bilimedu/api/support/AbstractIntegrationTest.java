package kz.bilimedu.api.support;

import java.util.List;
import java.util.Map;
import kz.bilimedu.api.auth.RefreshTokenRepository;
import kz.bilimedu.api.auth.dto.LoginRequest;
import kz.bilimedu.api.auth.dto.TokenPairResponse;
import kz.bilimedu.api.user.Role;
import kz.bilimedu.api.user.User;
import kz.bilimedu.api.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * Общая обвязка интеграционных тестов: настоящий PostgreSQL в контейнере,
 * настоящий HTTP на случайном порту, полная цепочка фильтров Spring Security.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    protected static final String PASSWORD = "correct-horse-battery";

    @LocalServerPort
    private int port;

    @Autowired
    protected UserRepository users;

    @Autowired
    protected RefreshTokenRepository refreshTokens;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    /** Анонимный клиент: ни одного заголовка авторизации. */
    protected RestTestClient client;

    @BeforeEach
    void prepare() {
        refreshTokens.deleteAll();
        users.deleteAll();
        client = RestTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    }

    /** Клиент, подставляющий Bearer-токен в каждый запрос. */
    protected RestTestClient authorized(String accessToken) {
        return RestTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .build();
    }

    protected User createUser(String email, Role role) {
        return users.save(new User("Тестовый Пользователь", email, passwordEncoder.encode(PASSWORD), role));
    }

    protected TokenPairResponse login(String email) {
        return client.post().uri("/api/v1/auth/login")
                .body(new LoginRequest(email, PASSWORD))
                .exchange()
                .returnResult(TokenPairResponse.class)
                .getResponseBody();
    }

    /** Тело ошибки из docs/api-v1.md. */
    public record ErrorEnvelope(ErrorBody error) {
        public record ErrorBody(String code, String message, List<Map<String, Object>> details) {
        }
    }
}
