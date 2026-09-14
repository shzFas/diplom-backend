package kz.bilimedu.api.enrollment;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import kz.bilimedu.api.enrollment.dto.CreateEnrollmentRequest;
import kz.bilimedu.api.enrollment.dto.EnrollmentResponse;
import kz.bilimedu.api.enrollment.dto.TransferRequest;
import kz.bilimedu.api.support.AbstractIntegrationTest;
import kz.bilimedu.api.user.Role;
import kz.bilimedu.api.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.client.EntityExchangeResult;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * Зачисления и переводы. В версии 2023 года перевод был перезаписью
 * Student.classId: история исчезала, а прошлогодние оценки начинали
 * выглядеть как оценки нового класса.
 */
class EnrollmentIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private EnrollmentRepository enrollments;

    private static final LocalDate SEPTEMBER = LocalDate.parse("2025-09-01");
    private static final LocalDate JANUARY = LocalDate.parse("2026-01-12");

    @Test
    @DisplayName("ADMIN зачисляет ученика: 201, зачисление активно")
    void adminEnrollsStudent() {
        RestTestClient admin = as("admin@school.kz", Role.ADMIN);
        User pupil = createUser("pupil@school.kz", Role.STUDENT);
        long schoolClass = createClass("8А");

        EntityExchangeResult<EnrollmentResponse> result = admin.post().uri("/api/v1/enrollments")
                .body(new CreateEnrollmentRequest(pupil.getId(), schoolClass, SEPTEMBER))
                .exchange().returnResult(EnrollmentResponse.class);

        assertThat(result.getStatus()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getResponseBody().active()).isTrue();
        assertThat(result.getResponseBody().className()).isEqualTo("8А");
        assertThat(result.getResponseBody().toDate()).isNull();
    }

    @Test
    @DisplayName("зачислить можно только активного пользователя с ролью STUDENT")
    void onlyActiveStudentCanBeEnrolled() {
        RestTestClient admin = as("admin@school.kz", Role.ADMIN);
        long schoolClass = createClass("8А");

        User teacher = createUser("teacher@school.kz", Role.TEACHER);
        EntityExchangeResult<ErrorEnvelope> asTeacher = admin.post().uri("/api/v1/enrollments")
                .body(new CreateEnrollmentRequest(teacher.getId(), schoolClass, SEPTEMBER))
                .exchange().returnResult(ErrorEnvelope.class);
        assertThat(asTeacher.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(asTeacher.getResponseBody().error().code()).isEqualTo("NOT_A_STUDENT");

        User expelled = createUser("expelled@school.kz", Role.STUDENT);
        expelled.deactivate();
        users.save(expelled);
        EntityExchangeResult<ErrorEnvelope> asExpelled = admin.post().uri("/api/v1/enrollments")
                .body(new CreateEnrollmentRequest(expelled.getId(), schoolClass, SEPTEMBER))
                .exchange().returnResult(ErrorEnvelope.class);
        assertThat(asExpelled.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
    }

    /** Правило временно́й модели: ограничение EXCLUDE USING gist, не проверка в коде. */
    @Test
    @DisplayName("в двух классах одновременно — 409 ENROLLMENT_OVERLAPS")
    void studentCannotBeInTwoClassesAtOnce() {
        RestTestClient admin = as("admin@school.kz", Role.ADMIN);
        User pupil = createUser("pupil@school.kz", Role.STUDENT);
        short year = createAcademicYear();
        long first = createClassIn(year, "8А");
        long second = createClassIn(year, "8Б");

        admin.post().uri("/api/v1/enrollments")
                .body(new CreateEnrollmentRequest(pupil.getId(), first, SEPTEMBER))
                .exchange().returnResult(String.class);

        EntityExchangeResult<ErrorEnvelope> overlap = admin.post().uri("/api/v1/enrollments")
                .body(new CreateEnrollmentRequest(pupil.getId(), second, JANUARY))
                .exchange().returnResult(ErrorEnvelope.class);

        assertThat(overlap.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(overlap.getResponseBody().error().code()).isEqualTo("ENROLLMENT_OVERLAPS");
    }

    @Test
    @DisplayName("перевод закрывает прежнее зачисление днём раньше и открывает новое")
    void transferClosesPreviousAndOpensNext() {
        RestTestClient admin = as("admin@school.kz", Role.ADMIN);
        User pupil = createUser("pupil@school.kz", Role.STUDENT);
        short year = createAcademicYear();
        long first = createClassIn(year, "8А");
        long second = createClassIn(year, "8Б");

        EnrollmentResponse initial = admin.post().uri("/api/v1/enrollments")
                .body(new CreateEnrollmentRequest(pupil.getId(), first, SEPTEMBER))
                .exchange().returnResult(EnrollmentResponse.class).getResponseBody();

        EnrollmentResponse moved = admin.post().uri("/api/v1/enrollments/" + initial.id() + "/transfer")
                .body(new TransferRequest(second, JANUARY))
                .exchange().returnResult(EnrollmentResponse.class).getResponseBody();

        assertThat(moved.className()).isEqualTo("8Б");
        assertThat(moved.fromDate()).isEqualTo(JANUARY);
        assertThat(moved.active()).isTrue();

        // Прежняя запись не удалена и не перезаписана: она закрыта днём раньше.
        Enrollment previous = enrollments.findById(Long.valueOf(initial.id())).orElseThrow();
        assertThat(previous.getToDate()).isEqualTo(JANUARY.minusDays(1));
        assertThat(previous.getClassId()).isEqualTo(first);
    }

    @Test
    @DisplayName("состав класса на дату: сентябрь показывает прежний класс, январь — новый")
    void rosterReflectsDate() {
        RestTestClient admin = as("admin@school.kz", Role.ADMIN);
        User pupil = createUser("pupil@school.kz", Role.STUDENT, "Асель Нурланова");
        short year = createAcademicYear();
        long first = createClassIn(year, "8А");
        long second = createClassIn(year, "8Б");

        EnrollmentResponse initial = admin.post().uri("/api/v1/enrollments")
                .body(new CreateEnrollmentRequest(pupil.getId(), first, SEPTEMBER))
                .exchange().returnResult(EnrollmentResponse.class).getResponseBody();
        admin.post().uri("/api/v1/enrollments/" + initial.id() + "/transfer")
                .body(new TransferRequest(second, JANUARY))
                .exchange().returnResult(String.class);

        assertThat(roster(admin, first, "2025-10-01").total())
                .as("в октябре ученик ещё в 8А").isEqualTo(1);
        assertThat(roster(admin, second, "2025-10-01").total())
                .as("в октябре 8Б пуст").isZero();

        assertThat(roster(admin, first, "2026-02-01").total())
                .as("в феврале 8А уже без него").isZero();
        assertThat(roster(admin, second, "2026-02-01").total())
                .as("в февраль он в 8Б").isEqualTo(1);

        // Граница включительна: последний день в прежнем классе — 11 января.
        assertThat(roster(admin, first, "2026-01-11").total()).isEqualTo(1);
        assertThat(roster(admin, first, "2026-01-12").total()).isZero();
    }

    @Test
    @DisplayName("история переводов отдаёт обе записи, от свежих к старым")
    void historyKeepsBothRecords() {
        RestTestClient admin = as("admin@school.kz", Role.ADMIN);
        User pupil = createUser("pupil@school.kz", Role.STUDENT);
        short year = createAcademicYear();
        long first = createClassIn(year, "8А");
        long second = createClassIn(year, "8Б");

        EnrollmentResponse initial = admin.post().uri("/api/v1/enrollments")
                .body(new CreateEnrollmentRequest(pupil.getId(), first, SEPTEMBER))
                .exchange().returnResult(EnrollmentResponse.class).getResponseBody();
        admin.post().uri("/api/v1/enrollments/" + initial.id() + "/transfer")
                .body(new TransferRequest(second, JANUARY))
                .exchange().returnResult(String.class);

        ItemsPage history = admin.get().uri("/api/v1/students/" + pupil.getId() + "/enrollments")
                .exchange().returnResult(ItemsPage.class).getResponseBody();

        assertThat(history.total()).isEqualTo(2);
        assertThat(history.field("className")).containsExactly("8Б", "8А");
        assertThat(history.field("active")).containsExactly(true, false);
    }

    @Test
    @DisplayName("перевод в тот же класс и перевод по закрытому зачислению отклоняются")
    void transferGuards() {
        RestTestClient admin = as("admin@school.kz", Role.ADMIN);
        User pupil = createUser("pupil@school.kz", Role.STUDENT);
        short year = createAcademicYear();
        long first = createClassIn(year, "8А");
        long second = createClassIn(year, "8Б");

        EnrollmentResponse initial = admin.post().uri("/api/v1/enrollments")
                .body(new CreateEnrollmentRequest(pupil.getId(), first, SEPTEMBER))
                .exchange().returnResult(EnrollmentResponse.class).getResponseBody();

        EntityExchangeResult<ErrorEnvelope> sameClass = admin.post()
                .uri("/api/v1/enrollments/" + initial.id() + "/transfer")
                .body(new TransferRequest(first, JANUARY))
                .exchange().returnResult(ErrorEnvelope.class);
        assertThat(sameClass.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(sameClass.getResponseBody().error().code()).isEqualTo("ALREADY_IN_CLASS");

        admin.post().uri("/api/v1/enrollments/" + initial.id() + "/transfer")
                .body(new TransferRequest(second, JANUARY))
                .exchange().returnResult(String.class);

        EntityExchangeResult<ErrorEnvelope> closed = admin.post()
                .uri("/api/v1/enrollments/" + initial.id() + "/transfer")
                .body(new TransferRequest(first, LocalDate.parse("2026-03-01")))
                .exchange().returnResult(ErrorEnvelope.class);
        assertThat(closed.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(closed.getResponseBody().error().code()).isEqualTo("ENROLLMENT_NOT_ACTIVE");
    }

    @Test
    @DisplayName("перевод датой раньше начала прежнего зачисления не проходит")
    void transferCannotPredateEnrollment() {
        RestTestClient admin = as("admin@school.kz", Role.ADMIN);
        User pupil = createUser("pupil@school.kz", Role.STUDENT);
        short year = createAcademicYear();
        long first = createClassIn(year, "8А");
        long second = createClassIn(year, "8Б");

        EnrollmentResponse initial = admin.post().uri("/api/v1/enrollments")
                .body(new CreateEnrollmentRequest(pupil.getId(), first, SEPTEMBER))
                .exchange().returnResult(EnrollmentResponse.class).getResponseBody();

        EntityExchangeResult<ErrorEnvelope> result = admin.post()
                .uri("/api/v1/enrollments/" + initial.id() + "/transfer")
                .body(new TransferRequest(second, SEPTEMBER))
                .exchange().returnResult(ErrorEnvelope.class);

        assertThat(result.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(result.getResponseBody().error().code()).isEqualTo("VALIDATION_FAILED");
    }

    @Test
    @DisplayName("учитель видит состав своего класса и не видит чужого")
    void teacherReadsOwnRosterOnly() {
        User teacher = createUser("teacher@school.kz", Role.TEACHER);
        User mine = createUser("mine@school.kz", Role.STUDENT, "Мой Ученик");
        User foreign = createUser("foreign@school.kz", Role.STUDENT, "Чужой Ученик");
        short year = createAcademicYear();
        long ownClass = createClassIn(year, "8А");
        long otherClass = createClassIn(year, "9Б");
        assignTeacher(teacher.getId(), ownClass, "Математика");
        enroll(mine.getId(), ownClass);
        enroll(foreign.getId(), otherClass);

        RestTestClient asTeacher = authorized(login("teacher@school.kz").accessToken());

        assertThat(roster(asTeacher, ownClass, "2025-10-01").field("fullName"))
                .containsExactly("Мой Ученик");

        EntityExchangeResult<ErrorEnvelope> denied = asTeacher.get()
                .uri("/api/v1/classes/" + otherClass + "/roster?on=2025-10-01")
                .exchange().returnResult(ErrorEnvelope.class);
        assertThat(denied.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(denied.getResponseBody().error().code()).isEqualTo("NOT_OWNER");
    }

    @Test
    @DisplayName("ученику состав класса не показывается вовсе — 403")
    void studentCannotReadRoster() {
        long schoolClass = createClass("8А");

        EntityExchangeResult<ErrorEnvelope> result = as("pupil@school.kz", Role.STUDENT)
                .get().uri("/api/v1/classes/" + schoolClass + "/roster")
                .exchange().returnResult(ErrorEnvelope.class);

        assertThat(result.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(result.getResponseBody().error().code()).isEqualTo("ROLE_FORBIDDEN");
    }

    @Test
    @DisplayName("ученик читает свою историю переводов и не читает чужую")
    void studentReadsOwnHistoryOnly() {
        User pupil = createUser("pupil@school.kz", Role.STUDENT);
        User other = createUser("other@school.kz", Role.STUDENT);
        long schoolClass = createClass("8А");
        enroll(pupil.getId(), schoolClass);
        enroll(other.getId(), schoolClass);

        RestTestClient asPupil = authorized(login("pupil@school.kz").accessToken());

        assertThat(asPupil.get().uri("/api/v1/students/" + pupil.getId() + "/enrollments")
                .exchange().returnResult(ItemsPage.class).getResponseBody().total()).isEqualTo(1);

        EntityExchangeResult<ErrorEnvelope> denied = asPupil.get()
                .uri("/api/v1/students/" + other.getId() + "/enrollments")
                .exchange().returnResult(ErrorEnvelope.class);
        assertThat(denied.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(denied.getResponseBody().error().code()).isEqualTo("NOT_OWNER");
    }

    @Test
    @DisplayName("зачисления и состав класса пишет только завуч")
    void onlyAdminWritesEnrollments() {
        User pupil = createUser("pupil@school.kz", Role.STUDENT);
        long schoolClass = createClass("8А");
        RestTestClient teacher = as("teacher@school.kz", Role.TEACHER);

        assertThat(teacher.post().uri("/api/v1/enrollments")
                .body(new CreateEnrollmentRequest(pupil.getId(), schoolClass, SEPTEMBER))
                .exchange().returnResult(String.class).getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("зачисления закрыты без токена")
    void enrollmentsRequireToken() {
        assertThat(client.get().uri("/api/v1/classes/1/roster").exchange()
                .returnResult(String.class).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(client.get().uri("/api/v1/students/1/enrollments").exchange()
                .returnResult(String.class).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private ItemsPage roster(RestTestClient viewer, long classId, String on) {
        return viewer.get().uri("/api/v1/classes/" + classId + "/roster?on=" + on)
                .exchange().returnResult(ItemsPage.class).getResponseBody();
    }
}
