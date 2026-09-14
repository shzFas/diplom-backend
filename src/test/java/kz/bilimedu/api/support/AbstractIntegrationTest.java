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
import org.springframework.jdbc.core.simple.JdbcClient;
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

    @Autowired
    protected JdbcClient jdbc;

    /** Анонимный клиент: ни одного заголовка авторизации. */
    protected RestTestClient client;

    /**
     * TRUNCATE, а не deleteAll(): teaching_assignments.teacher_id объявлен
     * с ON DELETE RESTRICT, поэтому удалять пользователей поштучно нельзя,
     * пока на них ссылаются назначения.
     */
    @BeforeEach
    void prepare() {
        jdbc.sql("TRUNCATE TABLE grade_audit, outbox, term_grades, attendance, grades, lessons, "
                + "enrollments, teaching_assignments, classes, subjects, terms, academic_years, "
                + "refresh_tokens, users RESTART IDENTITY CASCADE").update();
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
        return createUser(email, role, "Тестовый Пользователь");
    }

    protected User createUser(String email, Role role, String fullName) {
        return users.save(new User(fullName, email, passwordEncoder.encode(PASSWORD), role));
    }

    protected TokenPairResponse login(String email) {
        return client.post().uri("/api/v1/auth/login")
                .body(new LoginRequest(email, PASSWORD))
                .exchange()
                .returnResult(TokenPairResponse.class)
                .getResponseBody();
    }

    /** Клиент от имени только что заведённого пользователя. */
    protected RestTestClient as(String email, Role role) {
        createUser(email, role);
        return authorized(login(email).accessToken());
    }

    // --- школьная структура: сущностей JPA для этих таблиц ещё нет ---

    protected long createClass(String name) {
        Long yearId = jdbc.sql("INSERT INTO academic_years (name, starts_on, ends_on) "
                        + "VALUES (:name, DATE '2025-09-01', DATE '2026-05-25') "
                        + "ON CONFLICT (name) DO UPDATE SET name = EXCLUDED.name RETURNING id")
                .param("name", "2025–2026")
                .query(Long.class).single();

        return jdbc.sql("INSERT INTO classes (academic_year_id, name) VALUES (:year, :name) RETURNING id")
                .param("year", yearId).param("name", name)
                .query(Long.class).single();
    }

    protected void assignTeacher(long teacherId, long classId, String subject) {
        Long subjectId = jdbc.sql("INSERT INTO subjects (name) VALUES (:name) "
                        + "ON CONFLICT (name) DO UPDATE SET name = EXCLUDED.name RETURNING id")
                .param("name", subject)
                .query(Long.class).single();

        jdbc.sql("INSERT INTO teaching_assignments (class_id, subject_id, teacher_id) "
                        + "VALUES (:class, :subject, :teacher)")
                .param("class", classId).param("subject", subjectId).param("teacher", teacherId)
                .update();
    }

    protected void enroll(long studentId, long classId) {
        jdbc.sql("INSERT INTO enrollments (student_id, class_id, from_date) "
                        + "VALUES (:student, :class, DATE '2025-09-01')")
                .param("student", studentId).param("class", classId)
                .update();
    }

    /** Тело ошибки из docs/api-v1.md. */
    public record ErrorEnvelope(ErrorBody error) {
        public record ErrorBody(String code, String message, List<Map<String, Object>> details) {
        }
    }

    /** Страница из контракта: {items, page, size, total}. */
    public record UserPage(List<Map<String, Object>> items, int page, int size, long total) {
    }
}
