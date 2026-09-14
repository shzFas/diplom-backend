package kz.bilimedu.api.lesson;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import kz.bilimedu.api.enrollment.dto.CreateEnrollmentRequest;
import kz.bilimedu.api.enrollment.dto.EnrollmentResponse;
import kz.bilimedu.api.enrollment.dto.TransferRequest;
import kz.bilimedu.api.lesson.dto.CreateLessonRequest;
import kz.bilimedu.api.lesson.dto.LessonResponse;
import kz.bilimedu.api.lesson.dto.UpdateLessonRequest;
import kz.bilimedu.api.support.AbstractIntegrationTest;
import kz.bilimedu.api.user.Role;
import kz.bilimedu.api.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.client.EntityExchangeResult;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * Уроки — бывший Ktp. Здесь впервые работают триггеры схемы: дата урока
 * внутри своей четверти (D3) и один СОЧ на четверть (D5).
 */
class LessonIntegrationTest extends AbstractIntegrationTest {

    private static final LocalDate SEPTEMBER_15 = LocalDate.parse("2025-09-15");

    private CreateLessonRequest lesson(long assignmentId, short termId, String title,
                                       String date, AssessmentKind kind) {
        return new CreateLessonRequest(assignmentId, termId, title, LocalDate.parse(date), kind, (short) 10);
    }

    @Test
    @DisplayName("учитель заводит урок в своём назначении: 201")
    void teacherCreatesLesson() {
        User teacher = createUser("teacher@school.kz", Role.TEACHER);
        short year = createAcademicYear();
        short term = createTerm(year, 1, "2025-09-01", "2025-10-26");
        long assignment = assignTeacher(teacher.getId(), createClassIn(year, "8А"), "Математика");
        RestTestClient asTeacher = authorized(login("teacher@school.kz").accessToken());

        EntityExchangeResult<LessonResponse> result = asTeacher.post().uri("/api/v1/lessons")
                .body(lesson(assignment, term, "Дроби", "2025-09-15", AssessmentKind.LESSON))
                .exchange().returnResult(LessonResponse.class);

        assertThat(result.getStatus()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getResponseBody().title()).isEqualTo("Дроби");
        assertThat(result.getResponseBody().lessonDate()).isEqualTo(SEPTEMBER_15);
    }

    /** Правило D3 — триггер lesson_date_within_term, а не проверка в коде. */
    @Test
    @DisplayName("дата урока вне своей четверти — 422 LESSON_OUTSIDE_TERM")
    void lessonDateMustFallInsideItsTerm() {
        User teacher = createUser("teacher@school.kz", Role.TEACHER);
        short year = createAcademicYear();
        short term = createTerm(year, 1, "2025-09-01", "2025-10-26");
        long assignment = assignTeacher(teacher.getId(), createClassIn(year, "8А"), "Математика");
        RestTestClient asTeacher = authorized(login("teacher@school.kz").accessToken());

        EntityExchangeResult<ErrorEnvelope> result = asTeacher.post().uri("/api/v1/lessons")
                .body(lesson(assignment, term, "Дроби", "2025-12-01", AssessmentKind.LESSON))
                .exchange().returnResult(ErrorEnvelope.class);

        assertThat(result.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(result.getResponseBody().error().code()).isEqualTo("LESSON_OUTSIDE_TERM");
    }

    /** Правило D5 — частичный уникальный индекс lessons_one_soch_per_term_idx. */
    @Test
    @DisplayName("второй СОЧ в четверти — 409 SOCH_ALREADY_EXISTS, а СОР сколько угодно")
    void onlyOneSochPerTerm() {
        User teacher = createUser("teacher@school.kz", Role.TEACHER);
        short year = createAcademicYear();
        short term = createTerm(year, 1, "2025-09-01", "2025-10-26");
        long assignment = assignTeacher(teacher.getId(), createClassIn(year, "8А"), "Математика");
        RestTestClient asTeacher = authorized(login("teacher@school.kz").accessToken());

        assertThat(asTeacher.post().uri("/api/v1/lessons")
                .body(lesson(assignment, term, "СОЧ за четверть", "2025-10-20", AssessmentKind.SOCH))
                .exchange().returnResult(String.class).getStatus()).isEqualTo(HttpStatus.CREATED);

        EntityExchangeResult<ErrorEnvelope> second = asTeacher.post().uri("/api/v1/lessons")
                .body(lesson(assignment, term, "Ещё один СОЧ", "2025-10-21", AssessmentKind.SOCH))
                .exchange().returnResult(ErrorEnvelope.class);
        assertThat(second.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(second.getResponseBody().error().code()).isEqualTo("SOCH_ALREADY_EXISTS");

        for (String date : new String[]{"2025-09-20", "2025-10-01"}) {
            assertThat(asTeacher.post().uri("/api/v1/lessons")
                    .body(lesson(assignment, term, "СОР " + date, date, AssessmentKind.SOR))
                    .exchange().returnResult(String.class).getStatus())
                    .as("СОР на %s", date).isEqualTo(HttpStatus.CREATED);
        }
    }

    @Test
    @DisplayName("дубль урока в назначении на ту же дату — 409 LESSON_ALREADY_EXISTS")
    void duplicateLessonRejected() {
        User teacher = createUser("teacher@school.kz", Role.TEACHER);
        short year = createAcademicYear();
        short term = createTerm(year, 1, "2025-09-01", "2025-10-26");
        long assignment = assignTeacher(teacher.getId(), createClassIn(year, "8А"), "Математика");
        RestTestClient asTeacher = authorized(login("teacher@school.kz").accessToken());

        var request = lesson(assignment, term, "Дроби", "2025-09-15", AssessmentKind.LESSON);
        asTeacher.post().uri("/api/v1/lessons").body(request).exchange().returnResult(String.class);

        EntityExchangeResult<ErrorEnvelope> duplicate = asTeacher.post().uri("/api/v1/lessons")
                .body(request).exchange().returnResult(ErrorEnvelope.class);
        assertThat(duplicate.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(duplicate.getResponseBody().error().code()).isEqualTo("LESSON_ALREADY_EXISTS");
    }

    @Test
    @DisplayName("четверть чужого учебного года — 422 TERM_YEAR_MISMATCH")
    void termMustBelongToClassYear() {
        User teacher = createUser("teacher@school.kz", Role.TEACHER);
        short thisYear = createAcademicYear("2025–2026", "2025-09-01", "2026-05-25");
        short nextYear = createAcademicYear("2026–2027", "2026-09-01", "2027-05-25");
        short foreignTerm = createTerm(nextYear, 1, "2026-09-01", "2026-10-26");
        long assignment = assignTeacher(teacher.getId(), createClassIn(thisYear, "8А"), "Математика");
        RestTestClient asTeacher = authorized(login("teacher@school.kz").accessToken());

        EntityExchangeResult<ErrorEnvelope> result = asTeacher.post().uri("/api/v1/lessons")
                .body(lesson(assignment, foreignTerm, "Дроби", "2026-09-15", AssessmentKind.LESSON))
                .exchange().returnResult(ErrorEnvelope.class);

        assertThat(result.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(result.getResponseBody().error().code()).isEqualTo("TERM_YEAR_MISMATCH");
    }

    @Test
    @DisplayName("учитель не заводит уроки в чужом назначении — 403 NOT_OWNER")
    void teacherCannotPlanInForeignAssignment() {
        User teacher = createUser("teacher@school.kz", Role.TEACHER);
        User colleague = createUser("colleague@school.kz", Role.TEACHER);
        short year = createAcademicYear();
        short term = createTerm(year, 1, "2025-09-01", "2025-10-26");
        long foreign = assignTeacher(colleague.getId(), createClassIn(year, "9Б"), "Физика");
        RestTestClient asTeacher = authorized(login("teacher@school.kz").accessToken());

        EntityExchangeResult<ErrorEnvelope> result = asTeacher.post().uri("/api/v1/lessons")
                .body(lesson(foreign, term, "Оптика", "2025-09-15", AssessmentKind.LESSON))
                .exchange().returnResult(ErrorEnvelope.class);

        assertThat(result.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(result.getResponseBody().error().code()).isEqualTo("NOT_OWNER");
    }

    /** По матрице прав завуч журнал читает, но не ведёт. */
    @Test
    @DisplayName("ADMIN читает уроки, но не создаёт их — 403 ROLE_FORBIDDEN")
    void adminReadsButDoesNotPlan() {
        User teacher = createUser("teacher@school.kz", Role.TEACHER);
        short year = createAcademicYear();
        short term = createTerm(year, 1, "2025-09-01", "2025-10-26");
        long assignment = assignTeacher(teacher.getId(), createClassIn(year, "8А"), "Математика");
        createLesson(assignment, term, "Дроби", "2025-09-15");
        RestTestClient admin = as("admin@school.kz", Role.ADMIN);

        assertThat(admin.get().uri("/api/v1/lessons").exchange()
                .returnResult(ItemsPage.class).getResponseBody().total()).isEqualTo(1);

        EntityExchangeResult<ErrorEnvelope> write = admin.post().uri("/api/v1/lessons")
                .body(lesson(assignment, term, "Свой урок", "2025-09-16", AssessmentKind.LESSON))
                .exchange().returnResult(ErrorEnvelope.class);
        assertThat(write.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(write.getResponseBody().error().code()).isEqualTo("ROLE_FORBIDDEN");
    }

    @Test
    @DisplayName("учитель видит только уроки своих назначений")
    void teacherSeesOwnLessonsOnly() {
        User teacher = createUser("teacher@school.kz", Role.TEACHER);
        User colleague = createUser("colleague@school.kz", Role.TEACHER);
        short year = createAcademicYear();
        short term = createTerm(year, 1, "2025-09-01", "2025-10-26");
        long mine = assignTeacher(teacher.getId(), createClassIn(year, "8А"), "Математика");
        long foreign = assignTeacher(colleague.getId(), createClassIn(year, "9Б"), "Физика");
        createLesson(mine, term, "Дроби", "2025-09-15");
        createLesson(foreign, term, "Оптика", "2025-09-16");

        ItemsPage visible = authorized(login("teacher@school.kz").accessToken())
                .get().uri("/api/v1/lessons").exchange()
                .returnResult(ItemsPage.class).getResponseBody();

        assertThat(visible.field("title")).containsExactly("Дроби");
    }

    /**
     * Приёмочный критерий фазы 02: переведённый ученик видит сентябрьские
     * уроки прежнего класса и не видит сентябрьские уроки нового.
     */
    @Test
    @DisplayName("переведённый ученик видит уроки по дате, а не по текущему классу")
    void transferredStudentSeesLessonsByDate() {
        RestTestClient admin = as("admin@school.kz", Role.ADMIN);
        User teacher = createUser("teacher@school.kz", Role.TEACHER);
        User pupil = createUser("pupil@school.kz", Role.STUDENT);

        short year = createAcademicYear();
        short first = createTerm(year, 1, "2025-09-01", "2025-10-26");
        short third = createTerm(year, 3, "2026-01-12", "2026-03-22");
        long oldClass = createClassIn(year, "8А");
        long newClass = createClassIn(year, "8Б");
        long oldAssignment = assignTeacher(teacher.getId(), oldClass, "Математика");
        long newAssignment = assignTeacher(teacher.getId(), newClass, "Алгебра");

        // В каждом классе по сентябрьскому и по январскому уроку.
        createLesson(oldAssignment, first, "Сентябрь в 8А", "2025-09-15");
        createLesson(newAssignment, first, "Сентябрь в 8Б", "2025-09-16");
        createLesson(oldAssignment, third, "Январь в 8А", "2026-02-02");
        createLesson(newAssignment, third, "Январь в 8Б", "2026-02-03");

        EnrollmentResponse initial = admin.post().uri("/api/v1/enrollments")
                .body(new CreateEnrollmentRequest(pupil.getId(), oldClass, LocalDate.parse("2025-09-01")))
                .exchange().returnResult(EnrollmentResponse.class).getResponseBody();
        admin.post().uri("/api/v1/enrollments/" + initial.id() + "/transfer")
                .body(new TransferRequest(newClass, LocalDate.parse("2026-01-12")))
                .exchange().returnResult(String.class);

        ItemsPage visible = authorized(login("pupil@school.kz").accessToken())
                .get().uri("/api/v1/lessons").exchange()
                .returnResult(ItemsPage.class).getResponseBody();

        assertThat(visible.field("title"))
                .containsExactlyInAnyOrder("Сентябрь в 8А", "Январь в 8Б")
                .doesNotContain("Сентябрь в 8Б")
                .doesNotContain("Январь в 8А");
    }

    @Test
    @DisplayName("ученик не видит урок чужого класса и поштучно — 403 NOT_OWNER")
    void studentCannotReadForeignLesson() {
        User teacher = createUser("teacher@school.kz", Role.TEACHER);
        User pupil = createUser("pupil@school.kz", Role.STUDENT);
        short year = createAcademicYear();
        short term = createTerm(year, 1, "2025-09-01", "2025-10-26");
        long ownClass = createClassIn(year, "8А");
        long foreignClass = createClassIn(year, "9Б");
        long ownAssignment = assignTeacher(teacher.getId(), ownClass, "Математика");
        long foreignAssignment = assignTeacher(teacher.getId(), foreignClass, "Физика");
        long ownLesson = createLesson(ownAssignment, term, "Дроби", "2025-09-15");
        long foreignLesson = createLesson(foreignAssignment, term, "Оптика", "2025-09-16");
        enroll(pupil.getId(), ownClass);

        RestTestClient asPupil = authorized(login("pupil@school.kz").accessToken());

        assertThat(asPupil.get().uri("/api/v1/lessons/" + ownLesson).exchange()
                .returnResult(String.class).getStatus()).isEqualTo(HttpStatus.OK);

        EntityExchangeResult<ErrorEnvelope> denied = asPupil.get()
                .uri("/api/v1/lessons/" + foreignLesson).exchange().returnResult(ErrorEnvelope.class);
        assertThat(denied.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(denied.getResponseBody().error().code()).isEqualTo("NOT_OWNER");
    }

    @Test
    @DisplayName("PATCH правит название, дату и максимум; перенос вне четверти отклоняется")
    void patchUpdatesLesson() {
        User teacher = createUser("teacher@school.kz", Role.TEACHER);
        short year = createAcademicYear();
        short term = createTerm(year, 1, "2025-09-01", "2025-10-26");
        long assignment = assignTeacher(teacher.getId(), createClassIn(year, "8А"), "Математика");
        RestTestClient asTeacher = authorized(login("teacher@school.kz").accessToken());
        LessonResponse created = asTeacher.post().uri("/api/v1/lessons")
                .body(lesson(assignment, term, "Дроби", "2025-09-15", AssessmentKind.LESSON))
                .exchange().returnResult(LessonResponse.class).getResponseBody();

        LessonResponse patched = asTeacher.patch().uri("/api/v1/lessons/" + created.id())
                .body(new UpdateLessonRequest("Десятичные дроби", LocalDate.parse("2025-09-22"), (short) 20))
                .exchange().returnResult(LessonResponse.class).getResponseBody();
        assertThat(patched.title()).isEqualTo("Десятичные дроби");
        assertThat(patched.lessonDate()).isEqualTo(LocalDate.parse("2025-09-22"));
        assertThat(patched.maxScore()).isEqualTo((short) 20);

        EntityExchangeResult<ErrorEnvelope> outside = asTeacher.patch().uri("/api/v1/lessons/" + created.id())
                .body(new UpdateLessonRequest(null, LocalDate.parse("2025-12-01"), null))
                .exchange().returnResult(ErrorEnvelope.class);
        assertThat(outside.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(outside.getResponseBody().error().code()).isEqualTo("LESSON_OUTSIDE_TERM");
    }

    @Test
    @DisplayName("DELETE убирает урок и каскадом его оценки")
    void deleteRemovesLessonAndItsGrades() {
        User teacher = createUser("teacher@school.kz", Role.TEACHER);
        User pupil = createUser("pupil@school.kz", Role.STUDENT);
        short year = createAcademicYear();
        short term = createTerm(year, 1, "2025-09-01", "2025-10-26");
        long schoolClass = createClassIn(year, "8А");
        long assignment = assignTeacher(teacher.getId(), schoolClass, "Математика");
        long lessonId = createLesson(assignment, term, "Дроби", "2025-09-15");
        enroll(pupil.getId(), schoolClass);
        jdbc.sql("INSERT INTO grades (lesson_id, student_id, score, graded_by) "
                        + "VALUES (:lesson, :student, 8, :teacher)")
                .param("lesson", lessonId).param("student", pupil.getId())
                .param("teacher", teacher.getId()).update();

        RestTestClient asTeacher = authorized(login("teacher@school.kz").accessToken());
        assertThat(asTeacher.delete().uri("/api/v1/lessons/" + lessonId)
                .exchange().returnResult(Void.class).getStatus()).isEqualTo(HttpStatus.NO_CONTENT);

        assertThat(jdbc.sql("SELECT count(*) FROM lessons").query(Long.class).single()).isZero();
        assertThat(jdbc.sql("SELECT count(*) FROM grades").query(Long.class).single()).isZero();
    }

    @Test
    @DisplayName("фильтры assignmentId и termId сужают список")
    void listFilters() {
        User teacher = createUser("teacher@school.kz", Role.TEACHER);
        short year = createAcademicYear();
        short first = createTerm(year, 1, "2025-09-01", "2025-10-26");
        short second = createTerm(year, 2, "2025-11-03", "2025-12-28");
        long schoolClass = createClassIn(year, "8А");
        long math = assignTeacher(teacher.getId(), schoolClass, "Математика");
        long physics = assignTeacher(teacher.getId(), schoolClass, "Физика");
        createLesson(math, first, "Дроби", "2025-09-15");
        createLesson(math, second, "Проценты", "2025-11-10");
        createLesson(physics, first, "Оптика", "2025-09-16");

        RestTestClient asTeacher = authorized(login("teacher@school.kz").accessToken());

        assertThat(asTeacher.get().uri("/api/v1/lessons").exchange()
                .returnResult(ItemsPage.class).getResponseBody().total()).isEqualTo(3);
        assertThat(asTeacher.get().uri("/api/v1/lessons?assignmentId=" + math).exchange()
                .returnResult(ItemsPage.class).getResponseBody().total()).isEqualTo(2);
        assertThat(asTeacher.get().uri("/api/v1/lessons?termId=" + first).exchange()
                .returnResult(ItemsPage.class).getResponseBody().field("title"))
                .containsExactlyInAnyOrder("Дроби", "Оптика");
    }

    @Test
    @DisplayName("уроки закрыты без токена")
    void lessonsRequireToken() {
        assertThat(client.get().uri("/api/v1/lessons").exchange()
                .returnResult(String.class).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
