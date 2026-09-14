package kz.bilimedu.api.calendar;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import kz.bilimedu.api.calendar.dto.CreateAcademicYearRequest;
import kz.bilimedu.api.calendar.dto.CreateTermRequest;
import kz.bilimedu.api.calendar.dto.TermResponse;
import kz.bilimedu.api.support.AbstractIntegrationTest;
import kz.bilimedu.api.user.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.client.EntityExchangeResult;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * Учебный календарь. В версии 2023 года четверть была строкой ktpPeriod,
 * продублированной в каждом уроке и в каждой оценке: нельзя было ни проверить
 * дату урока, ни закрыть четверть, ни отчитаться за год.
 */
class CalendarIntegrationTest extends AbstractIntegrationTest {

    private static final CreateAcademicYearRequest YEAR_2025 =
            new CreateAcademicYearRequest("2025–2026",
                    java.time.LocalDate.parse("2025-09-01"),
                    java.time.LocalDate.parse("2026-05-25"));

    private String createYearViaApi(RestTestClient admin) {
        return admin.post().uri("/api/v1/academic-years").body(YEAR_2025).exchange()
                .returnResult(Map.class).getResponseBody().get("id").toString();
    }

    @Test
    @DisplayName("ADMIN заводит учебный год: 201, id строкой")
    void adminCreatesAcademicYear() {
        EntityExchangeResult<String> result = as("admin@school.kz", Role.ADMIN)
                .post().uri("/api/v1/academic-years").body(YEAR_2025)
                .exchange().returnResult(String.class);

        assertThat(result.getStatus()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getResponseBody()).containsPattern("\"id\"\\s*:\\s*\"\\d+\"")
                .contains("2025–2026");
    }

    @Test
    @DisplayName("дубль названия года — 409 от ограничения базы")
    void duplicateYearRejected() {
        RestTestClient admin = as("admin@school.kz", Role.ADMIN);
        createYearViaApi(admin);

        EntityExchangeResult<ErrorEnvelope> result = admin.post().uri("/api/v1/academic-years")
                .body(YEAR_2025).exchange().returnResult(ErrorEnvelope.class);

        assertThat(result.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(result.getResponseBody().error().code()).isEqualTo("ACADEMIC_YEAR_ALREADY_EXISTS");
    }

    @Test
    @DisplayName("год, который кончается раньше, чем начинается, не принимается")
    void yearWithInvertedDatesRejected() {
        EntityExchangeResult<ErrorEnvelope> result = as("admin@school.kz", Role.ADMIN)
                .post().uri("/api/v1/academic-years")
                .body(new CreateAcademicYearRequest("Плохой год",
                        java.time.LocalDate.parse("2026-05-25"),
                        java.time.LocalDate.parse("2025-09-01")))
                .exchange().returnResult(ErrorEnvelope.class);

        assertThat(result.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(result.getResponseBody().error().code()).isEqualTo("VALIDATION_FAILED");
    }

    @Test
    @DisplayName("четыре четверти года заводятся, пятая не проходит валидацию")
    void fourTermsFitOneYear() {
        RestTestClient admin = as("admin@school.kz", Role.ADMIN);
        short year = Short.parseShort(createYearViaApi(admin));

        String[][] quarters = {
                {"1", "2025-09-01", "2025-10-26"},
                {"2", "2025-11-03", "2025-12-28"},
                {"3", "2026-01-12", "2026-03-22"},
                {"4", "2026-04-01", "2026-05-25"},
        };
        for (String[] quarter : quarters) {
            EntityExchangeResult<String> created = admin.post().uri("/api/v1/terms")
                    .body(new CreateTermRequest(year, Short.valueOf(quarter[0]),
                            java.time.LocalDate.parse(quarter[1]), java.time.LocalDate.parse(quarter[2])))
                    .exchange().returnResult(String.class);
            assertThat(created.getStatus()).as("четверть %s", quarter[0]).isEqualTo(HttpStatus.CREATED);
        }

        // Правило D1: ordinal BETWEEN 1 AND 4 — и в базе, и в DTO.
        EntityExchangeResult<ErrorEnvelope> fifth = admin.post().uri("/api/v1/terms")
                .body(new CreateTermRequest(year, (short) 5,
                        java.time.LocalDate.parse("2026-05-26"), java.time.LocalDate.parse("2026-05-27")))
                .exchange().returnResult(ErrorEnvelope.class);
        assertThat(fifth.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    /** Правило D2 — ограничение EXCLUDE USING gist, а не проверка в коде. */
    @Test
    @DisplayName("пересекающиеся четверти — 409 TERM_OVERLAPS")
    void overlappingTermsRejected() {
        RestTestClient admin = as("admin@school.kz", Role.ADMIN);
        short year = Short.parseShort(createYearViaApi(admin));

        admin.post().uri("/api/v1/terms").body(new CreateTermRequest(year, (short) 1,
                        java.time.LocalDate.parse("2025-09-01"), java.time.LocalDate.parse("2025-10-26")))
                .exchange().returnResult(String.class);

        EntityExchangeResult<ErrorEnvelope> overlap = admin.post().uri("/api/v1/terms")
                .body(new CreateTermRequest(year, (short) 2,
                        java.time.LocalDate.parse("2025-10-20"), java.time.LocalDate.parse("2025-12-28")))
                .exchange().returnResult(ErrorEnvelope.class);

        assertThat(overlap.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(overlap.getResponseBody().error().code()).isEqualTo("TERM_OVERLAPS");
    }

    @Test
    @DisplayName("повторный номер четверти в году — 409 TERM_ORDINAL_TAKEN")
    void duplicateOrdinalRejected() {
        RestTestClient admin = as("admin@school.kz", Role.ADMIN);
        short year = Short.parseShort(createYearViaApi(admin));

        admin.post().uri("/api/v1/terms").body(new CreateTermRequest(year, (short) 1,
                        java.time.LocalDate.parse("2025-09-01"), java.time.LocalDate.parse("2025-10-26")))
                .exchange().returnResult(String.class);

        EntityExchangeResult<ErrorEnvelope> again = admin.post().uri("/api/v1/terms")
                .body(new CreateTermRequest(year, (short) 1,
                        java.time.LocalDate.parse("2025-11-03"), java.time.LocalDate.parse("2025-12-28")))
                .exchange().returnResult(ErrorEnvelope.class);

        assertThat(again.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(again.getResponseBody().error().code()).isEqualTo("TERM_ORDINAL_TAKEN");
    }

    @Test
    @DisplayName("четверть за пределами своего учебного года не принимается")
    void termOutsideYearRejected() {
        RestTestClient admin = as("admin@school.kz", Role.ADMIN);
        short year = Short.parseShort(createYearViaApi(admin));

        EntityExchangeResult<ErrorEnvelope> result = admin.post().uri("/api/v1/terms")
                .body(new CreateTermRequest(year, (short) 1,
                        java.time.LocalDate.parse("2025-08-01"), java.time.LocalDate.parse("2025-10-26")))
                .exchange().returnResult(ErrorEnvelope.class);

        assertThat(result.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("close и reopen переключают признак закрытия и идемпотентны")
    void closeAndReopenTerm() {
        RestTestClient admin = as("admin@school.kz", Role.ADMIN);
        short year = createAcademicYear();
        short term = createTerm(year, 1, "2025-09-01", "2025-10-26");

        TermResponse closed = admin.post().uri("/api/v1/terms/" + term + "/close")
                .exchange().returnResult(TermResponse.class).getResponseBody();
        assertThat(closed.closed()).isTrue();
        assertThat(closed.closedAt()).isNotNull();

        TermResponse closedAgain = admin.post().uri("/api/v1/terms/" + term + "/close")
                .exchange().returnResult(TermResponse.class).getResponseBody();
        assertThat(closedAgain.closedAt()).isEqualTo(closed.closedAt());

        TermResponse reopened = admin.post().uri("/api/v1/terms/" + term + "/reopen")
                .exchange().returnResult(TermResponse.class).getResponseBody();
        assertThat(reopened.closed()).isFalse();
        assertThat(reopened.closedAt()).isNull();
    }

    @Test
    @DisplayName("календарь читают все роли, менять его может только завуч")
    void calendarIsReadableByEveryoneAndWritableByAdmin() {
        short year = createAcademicYear();
        createTerm(year, 1, "2025-09-01", "2025-10-26");

        for (Role role : new Role[]{Role.TEACHER, Role.STUDENT}) {
            RestTestClient viewer = as(role.name().toLowerCase() + "@school.kz", role);

            ItemsPage terms = viewer.get().uri("/api/v1/terms").exchange()
                    .returnResult(ItemsPage.class).getResponseBody();
            assertThat(terms.total()).as("чтение четвертей для %s", role).isEqualTo(1);

            EntityExchangeResult<ErrorEnvelope> write = viewer.post().uri("/api/v1/academic-years")
                    .body(new CreateAcademicYearRequest("Чужой год",
                            java.time.LocalDate.parse("2030-09-01"),
                            java.time.LocalDate.parse("2031-05-25")))
                    .exchange().returnResult(ErrorEnvelope.class);
            assertThat(write.getStatus()).as("запись для %s", role).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(write.getResponseBody().error().code()).isEqualTo("ROLE_FORBIDDEN");
        }
    }

    @Test
    @DisplayName("фильтр academicYearId сужает список четвертей")
    void termsFilteredByYear() {
        short first = createAcademicYear("2025–2026", "2025-09-01", "2026-05-25");
        short second = createAcademicYear("2026–2027", "2026-09-01", "2027-05-25");
        createTerm(first, 1, "2025-09-01", "2025-10-26");
        createTerm(second, 1, "2026-09-01", "2026-10-26");
        createTerm(second, 2, "2026-11-03", "2026-12-28");

        RestTestClient admin = as("admin@school.kz", Role.ADMIN);

        assertThat(admin.get().uri("/api/v1/terms").exchange()
                .returnResult(ItemsPage.class).getResponseBody().total()).isEqualTo(3);
        assertThat(admin.get().uri("/api/v1/terms?academicYearId=" + second).exchange()
                .returnResult(ItemsPage.class).getResponseBody().total()).isEqualTo(2);
    }

    @Test
    @DisplayName("календарь закрыт без токена")
    void calendarRequiresToken() {
        assertThat(client.get().uri("/api/v1/academic-years").exchange()
                .returnResult(String.class).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(client.get().uri("/api/v1/terms").exchange()
                .returnResult(String.class).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
