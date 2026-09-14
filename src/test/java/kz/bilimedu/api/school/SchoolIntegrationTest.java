package kz.bilimedu.api.school;

import static org.assertj.core.api.Assertions.assertThat;

import kz.bilimedu.api.school.dto.CreateAssignmentRequest;
import kz.bilimedu.api.school.dto.CreateClassRequest;
import kz.bilimedu.api.school.dto.CreateSubjectRequest;
import kz.bilimedu.api.support.AbstractIntegrationTest;
import kz.bilimedu.api.user.Role;
import kz.bilimedu.api.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.client.EntityExchangeResult;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * Классы, предметы и назначения. Заменяют два нетипизированных массива
 * версии 2023 года — Predmet.classes[] и User.permission[], — которые
 * нельзя было ни проверить запросом, ни использовать для авторизации.
 */
class SchoolIntegrationTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("один и тот же класс живёт в каждом году отдельно")
    void classNameIsUniquePerYearOnly() {
        RestTestClient admin = as("admin@school.kz", Role.ADMIN);
        short first = createAcademicYear("2025–2026", "2025-09-01", "2026-05-25");
        short second = createAcademicYear("2026–2027", "2026-09-01", "2027-05-25");

        assertThat(admin.post().uri("/api/v1/classes").body(new CreateClassRequest(first, "8А"))
                .exchange().returnResult(String.class).getStatus()).isEqualTo(HttpStatus.CREATED);

        // Тот же «8А», но другой учебный год — законная отдельная строка.
        assertThat(admin.post().uri("/api/v1/classes").body(new CreateClassRequest(second, "8А"))
                .exchange().returnResult(String.class).getStatus()).isEqualTo(HttpStatus.CREATED);

        EntityExchangeResult<ErrorEnvelope> duplicate = admin.post().uri("/api/v1/classes")
                .body(new CreateClassRequest(first, "8А"))
                .exchange().returnResult(ErrorEnvelope.class);
        assertThat(duplicate.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(duplicate.getResponseBody().error().code()).isEqualTo("CLASS_ALREADY_EXISTS");
    }

    @Test
    @DisplayName("класс в несуществующем году — 404")
    void classNeedsExistingYear() {
        EntityExchangeResult<ErrorEnvelope> result = as("admin@school.kz", Role.ADMIN)
                .post().uri("/api/v1/classes").body(new CreateClassRequest((short) 999, "8А"))
                .exchange().returnResult(ErrorEnvelope.class);

        assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("дубль предмета — 409 SUBJECT_ALREADY_EXISTS")
    void duplicateSubjectRejected() {
        RestTestClient admin = as("admin@school.kz", Role.ADMIN);
        admin.post().uri("/api/v1/subjects").body(new CreateSubjectRequest("Математика"))
                .exchange().returnResult(String.class);

        EntityExchangeResult<ErrorEnvelope> result = admin.post().uri("/api/v1/subjects")
                .body(new CreateSubjectRequest("Математика"))
                .exchange().returnResult(ErrorEnvelope.class);

        assertThat(result.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(result.getResponseBody().error().code()).isEqualTo("SUBJECT_ALREADY_EXISTS");
    }

    @Test
    @DisplayName("назначение учителя: 201, повторное на ту же пару — 409")
    void assignmentIsUniquePerClassAndSubject() {
        RestTestClient admin = as("admin@school.kz", Role.ADMIN);
        User teacher = createUser("teacher@school.kz", Role.TEACHER);
        long schoolClass = createClass("8А");
        long subject = createSubject("Математика");

        var request = new CreateAssignmentRequest(schoolClass, subject, teacher.getId());
        assertThat(admin.post().uri("/api/v1/teaching-assignments").body(request)
                .exchange().returnResult(String.class).getStatus()).isEqualTo(HttpStatus.CREATED);

        EntityExchangeResult<ErrorEnvelope> duplicate = admin.post().uri("/api/v1/teaching-assignments")
                .body(request).exchange().returnResult(ErrorEnvelope.class);
        assertThat(duplicate.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(duplicate.getResponseBody().error().code()).isEqualTo("ASSIGNMENT_ALREADY_EXISTS");
    }

    @Test
    @DisplayName("вести предмет может только активный пользователь с ролью TEACHER")
    void onlyActiveTeacherCanBeAssigned() {
        RestTestClient admin = as("admin@school.kz", Role.ADMIN);
        long schoolClass = createClass("8А");
        long subject = createSubject("Математика");

        User pupil = createUser("pupil@school.kz", Role.STUDENT);
        EntityExchangeResult<ErrorEnvelope> asStudent = admin.post().uri("/api/v1/teaching-assignments")
                .body(new CreateAssignmentRequest(schoolClass, subject, pupil.getId()))
                .exchange().returnResult(ErrorEnvelope.class);
        assertThat(asStudent.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(asStudent.getResponseBody().error().code()).isEqualTo("NOT_A_TEACHER");

        User fired = createUser("fired@school.kz", Role.TEACHER);
        fired.deactivate();
        users.save(fired);
        EntityExchangeResult<ErrorEnvelope> asFired = admin.post().uri("/api/v1/teaching-assignments")
                .body(new CreateAssignmentRequest(schoolClass, subject, fired.getId()))
                .exchange().returnResult(ErrorEnvelope.class);
        assertThat(asFired.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(asFired.getResponseBody().error().code()).isEqualTo("NOT_A_TEACHER");
    }

    /**
     * Уроки ссылаются на назначение с ON DELETE CASCADE: без этой проверки
     * удаление назначения молча унесло бы журнал вместе с оценками.
     */
    @Test
    @DisplayName("назначение с уроками не удаляется — 409 ASSIGNMENT_HAS_LESSONS")
    void assignmentWithLessonsIsProtected() {
        RestTestClient admin = as("admin@school.kz", Role.ADMIN);
        User teacher = createUser("teacher@school.kz", Role.TEACHER);
        short year = createAcademicYear();
        short term = createTerm(year, 1, "2025-09-01", "2025-10-26");
        long schoolClass = createClassIn(year, "8А");
        long assignment = assignTeacher(teacher.getId(), schoolClass, "Математика");

        long empty = assignTeacher(teacher.getId(), schoolClass, "Физика");
        assertThat(admin.delete().uri("/api/v1/teaching-assignments/" + empty)
                .exchange().returnResult(Void.class).getStatus()).isEqualTo(HttpStatus.NO_CONTENT);

        createLesson(assignment, term, "Дроби", "2025-09-15");
        EntityExchangeResult<ErrorEnvelope> protectedDelete = admin.delete()
                .uri("/api/v1/teaching-assignments/" + assignment)
                .exchange().returnResult(ErrorEnvelope.class);

        assertThat(protectedDelete.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(protectedDelete.getResponseBody().error().code()).isEqualTo("ASSIGNMENT_HAS_LESSONS");
        assertThat(jdbc.sql("SELECT count(*) FROM lessons").query(Long.class).single()).isEqualTo(1);
    }

    @Test
    @DisplayName("учитель видит свои классы и назначения, но не чужие")
    void teacherSeesOnlyOwnClassesAndAssignments() {
        User teacher = createUser("teacher@school.kz", Role.TEACHER);
        User colleague = createUser("colleague@school.kz", Role.TEACHER);
        short year = createAcademicYear();
        long mine = createClassIn(year, "8А");
        long foreign = createClassIn(year, "9Б");
        assignTeacher(teacher.getId(), mine, "Математика");
        assignTeacher(colleague.getId(), foreign, "Физика");

        RestTestClient asTeacher = authorized(login("teacher@school.kz").accessToken());

        ItemsPage classes = asTeacher.get().uri("/api/v1/classes").exchange()
                .returnResult(ItemsPage.class).getResponseBody();
        assertThat(classes.field("name")).containsExactly("8А");

        ItemsPage assignments = asTeacher.get().uri("/api/v1/teaching-assignments").exchange()
                .returnResult(ItemsPage.class).getResponseBody();
        assertThat(assignments.total()).isEqualTo(1);
        assertThat(assignments.field("teacherId")).containsExactly(String.valueOf(teacher.getId()));
    }

    @Test
    @DisplayName("ученик видит свой текущий класс и предметы этого класса")
    void studentSeesOwnClassAndItsSubjects() {
        User teacher = createUser("teacher@school.kz", Role.TEACHER);
        User pupil = createUser("pupil@school.kz", Role.STUDENT);
        short year = createAcademicYear();
        long mine = createClassIn(year, "8А");
        long foreign = createClassIn(year, "9Б");
        assignTeacher(teacher.getId(), mine, "Математика");
        assignTeacher(teacher.getId(), foreign, "Биология");
        enroll(pupil.getId(), mine);

        RestTestClient asPupil = authorized(login("pupil@school.kz").accessToken());

        assertThat(asPupil.get().uri("/api/v1/classes").exchange()
                .returnResult(ItemsPage.class).getResponseBody().field("name"))
                .containsExactly("8А");

        assertThat(asPupil.get().uri("/api/v1/subjects").exchange()
                .returnResult(ItemsPage.class).getResponseBody().field("name"))
                .containsExactly("Математика");
    }

    @Test
    @DisplayName("ученику назначения не показываются вовсе — 403")
    void studentCannotSeeAssignments() {
        EntityExchangeResult<ErrorEnvelope> result = as("pupil@school.kz", Role.STUDENT)
                .get().uri("/api/v1/teaching-assignments")
                .exchange().returnResult(ErrorEnvelope.class);

        assertThat(result.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(result.getResponseBody().error().code()).isEqualTo("ROLE_FORBIDDEN");
    }

    @Test
    @DisplayName("учитель не заводит классы, предметы и назначения — это право завуча")
    void teacherCannotWriteReferenceData() {
        RestTestClient teacher = as("teacher@school.kz", Role.TEACHER);
        short year = createAcademicYear();

        assertThat(teacher.post().uri("/api/v1/classes").body(new CreateClassRequest(year, "8А"))
                .exchange().returnResult(String.class).getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(teacher.post().uri("/api/v1/subjects").body(new CreateSubjectRequest("Химия"))
                .exchange().returnResult(String.class).getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(teacher.post().uri("/api/v1/teaching-assignments")
                .body(new CreateAssignmentRequest(1L, 1L, 1L))
                .exchange().returnResult(String.class).getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("фильтр classId отдаёт предметы конкретного класса")
    void subjectsFilteredByClass() {
        RestTestClient admin = as("admin@school.kz", Role.ADMIN);
        User teacher = createUser("teacher@school.kz", Role.TEACHER);
        short year = createAcademicYear();
        long first = createClassIn(year, "8А");
        long second = createClassIn(year, "9Б");
        assignTeacher(teacher.getId(), first, "Математика");
        assignTeacher(teacher.getId(), second, "Биология");

        assertThat(admin.get().uri("/api/v1/subjects").exchange()
                .returnResult(ItemsPage.class).getResponseBody().total()).isEqualTo(2);
        assertThat(admin.get().uri("/api/v1/subjects?classId=" + first).exchange()
                .returnResult(ItemsPage.class).getResponseBody().field("name"))
                .containsExactly("Математика");
    }

    @Test
    @DisplayName("справочники закрыты без токена")
    void referenceDataRequiresToken() {
        for (String uri : new String[]{"/api/v1/classes", "/api/v1/subjects", "/api/v1/teaching-assignments"}) {
            assertThat(client.get().uri(uri).exchange().returnResult(String.class).getStatus())
                    .as(uri).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }
}
