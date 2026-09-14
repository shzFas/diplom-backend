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

    // --- школьная структура: ставится напрямую в базу, чтобы тесты модуля
    //     не зависели от корректности других его же ручек ---

    protected short createAcademicYear() {
        return createAcademicYear("2025\u20132026", "2025-09-01", "2026-05-25");
    }

    protected short createAcademicYear(String name, String startsOn, String endsOn) {
        return jdbc.sql("INSERT INTO academic_years (name, starts_on, ends_on) "
                        + "VALUES (:name, CAST(:from AS date), CAST(:to AS date)) "
                        + "ON CONFLICT (name) DO UPDATE SET name = EXCLUDED.name RETURNING id")
                .param("name", name).param("from", startsOn).param("to", endsOn)
                .query(Short.class).single();
    }

    protected short createTerm(short academicYearId, int ordinal, String startsOn, String endsOn) {
        return jdbc.sql("INSERT INTO terms (academic_year_id, ordinal, starts_on, ends_on) "
                        + "VALUES (:year, :ordinal, CAST(:from AS date), CAST(:to AS date)) RETURNING id")
                .param("year", academicYearId).param("ordinal", ordinal)
                .param("from", startsOn).param("to", endsOn)
                .query(Short.class).single();
    }

    protected long createClass(String name) {
        return createClassIn(createAcademicYear(), name);
    }

    protected long createClassIn(short academicYearId, String name) {
        return jdbc.sql("INSERT INTO classes (academic_year_id, name) VALUES (:year, :name) RETURNING id")
                .param("year", academicYearId).param("name", name)
                .query(Long.class).single();
    }

    protected long createSubject(String name) {
        return jdbc.sql("INSERT INTO subjects (name) VALUES (:name) "
                        + "ON CONFLICT (name) DO UPDATE SET name = EXCLUDED.name RETURNING id")
                .param("name", name)
                .query(Long.class).single();
    }

    protected long assignTeacher(long teacherId, long classId, String subject) {
        return jdbc.sql("INSERT INTO teaching_assignments (class_id, subject_id, teacher_id) "
                        + "VALUES (:class, :subject, :teacher) RETURNING id")
                .param("class", classId).param("subject", createSubject(subject))
                .param("teacher", teacherId)
                .query(Long.class).single();
    }

    protected void enroll(long studentId, long classId) {
        jdbc.sql("INSERT INTO enrollments (student_id, class_id, from_date) "
                        + "VALUES (:student, :class, DATE '2025-09-01')")
                .param("student", studentId).param("class", classId)
                .update();
    }

    protected long createLesson(long assignmentId, short termId, String title, String lessonDate) {
        return jdbc.sql("INSERT INTO lessons (assignment_id, term_id, title, lesson_date, kind, max_score) "
                        + "VALUES (:assignment, :term, :title, CAST(:date AS date), "
                        + "CAST('LESSON' AS assessment_kind), 10) RETURNING id")
                .param("assignment", assignmentId).param("term", termId)
                .param("title", title).param("date", lessonDate)
                .query(Long.class).single();
    }

    /** Тело ошибки из docs/api-v1.md. */
    public record ErrorEnvelope(ErrorBody error) {
        public record ErrorBody(String code, String message, List<Map<String, Object>> details) {
        }
    }

    /** Страница из контракта: {items, page, size, total}. */
    public record UserPage(List<Map<String, Object>> items, int page, int size, long total) {
    }

    /** Та же страница для любых других коллекций — поля контракта одинаковы. */
    public record ItemsPage(List<Map<String, Object>> items, int page, int size, long total) {

        public List<Object> field(String name) {
            return items.stream().map(item -> item.get(name)).toList();
        }
    }
}
