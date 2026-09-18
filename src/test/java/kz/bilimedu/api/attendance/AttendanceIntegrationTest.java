package kz.bilimedu.api.attendance;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import kz.bilimedu.api.attendance.dto.AttendanceEntryRequest;
import kz.bilimedu.api.grade.dto.GradeEntryRequest;
import kz.bilimedu.api.support.AbstractIntegrationTest;
import kz.bilimedu.api.user.Role;
import kz.bilimedu.api.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.client.EntityExchangeResult;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * Посещаемость. Заменяет булев markFalse версии 2023 года, который
 * не отличал уважительную причину от прогула.
 */
class AttendanceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private AttendanceRepository attendance;

    private User teacher;
    private User pupil;
    private User otherPupil;
    private short academicYear;
    private short term;
    private long schoolClass;
    private long assignment;
    private long lessonId;
    private RestTestClient asTeacher;

    @BeforeEach
    void setUpSchool() {
        teacher = createUser("teacher@school.kz", Role.TEACHER, "Айгуль Смагулова");
        pupil = createUser("pupil@school.kz", Role.STUDENT, "Асель Нурланова");
        otherPupil = createUser("other@school.kz", Role.STUDENT, "Бекзат Амиров");

        academicYear = createAcademicYear();
        term = createTerm(academicYear, 1, "2025-09-01", "2025-10-26");
        schoolClass = createClassIn(academicYear, "8А");
        assignment = assignTeacher(teacher.getId(), schoolClass, "Математика");
        lessonId = createLesson(assignment, term, "Дроби", "2025-09-15");
        enroll(pupil.getId(), schoolClass);
        enroll(otherPupil.getId(), schoolClass);

        asTeacher = authorized(login("teacher@school.kz").accessToken());
    }

    private EntityExchangeResult<String> put(RestTestClient as, Object body) {
        return as.put().uri("/api/v1/lessons/" + lessonId + "/attendance")
                .body(body).exchange().returnResult(String.class);
    }

    private EntityExchangeResult<ErrorEnvelope> putExpectingError(RestTestClient as, Object body) {
        return as.put().uri("/api/v1/lessons/" + lessonId + "/attendance")
                .body(body).exchange().returnResult(ErrorEnvelope.class);
    }

    @Test
    @DisplayName("учитель закрывает ведомость класса одним запросом")
    void teacherMarksWholeClass() {
        assertThat(put(asTeacher, List.of(
                new AttendanceEntryRequest(pupil.getId(), AttendanceStatus.EXCUSED),
                new AttendanceEntryRequest(otherPupil.getId(), AttendanceStatus.PRESENT)))
                .getStatus()).isEqualTo(HttpStatus.OK);

        assertThat(attendance.count()).isEqualTo(2);
        assertThat(attendance.findByLessonIdAndStudentId(lessonId, pupil.getId()))
                .get().satisfies(mark -> {
                    assertThat(mark.getStatus()).isEqualTo(AttendanceStatus.EXCUSED);
                    assertThat(mark.getNotedBy()).isEqualTo(teacher.getId());
                });
    }

    /** Правило D11: четыре статуса, а не булев флаг. */
    @Test
    @DisplayName("все четыре статуса различимы, включая болезнь и прогул")
    void allFourStatusesAreDistinct() {
        long second = createLesson(assignment, term, "Проценты", "2025-09-22");
        long third = createLesson(assignment, term, "Уравнения", "2025-09-29");
        long fourth = createLesson(assignment, term, "Графики", "2025-10-06");

        record Case(long lesson, AttendanceStatus status) {
        }
        for (Case testCase : new Case[]{
                new Case(lessonId, AttendanceStatus.PRESENT),
                new Case(second, AttendanceStatus.EXCUSED),
                new Case(third, AttendanceStatus.UNEXCUSED),
                new Case(fourth, AttendanceStatus.LATE)}) {
            asTeacher.put().uri("/api/v1/lessons/" + testCase.lesson() + "/attendance")
                    .body(List.of(new AttendanceEntryRequest(pupil.getId(), testCase.status())))
                    .exchange().returnResult(String.class);
        }

        ItemsPage summary = asTeacher.get().uri("/api/v1/students/" + pupil.getId() + "/attendance")
                .exchange().returnResult(ItemsPage.class).getResponseBody();

        assertThat(summary.field("status"))
                .containsExactlyInAnyOrder("PRESENT", "EXCUSED", "UNEXCUSED", "LATE");
    }

    /** Правило D10: посещаемость и оценка независимы, возможны все сочетания. */
    @Test
    @DisplayName("отсутствующий ученик может иметь оценку за отработку")
    void attendanceAndGradeAreIndependent() {
        put(asTeacher, List.of(new AttendanceEntryRequest(pupil.getId(), AttendanceStatus.EXCUSED)));
        asTeacher.put().uri("/api/v1/lessons/" + lessonId + "/grades")
                .body(List.of(new GradeEntryRequest(pupil.getId(), (short) 9)))
                .exchange().returnResult(String.class);

        ItemsPage marks = asTeacher.get().uri("/api/v1/students/" + pupil.getId() + "/attendance")
                .exchange().returnResult(ItemsPage.class).getResponseBody();
        ItemsPage diary = asTeacher.get().uri("/api/v1/students/" + pupil.getId() + "/grades")
                .exchange().returnResult(ItemsPage.class).getResponseBody();

        assertThat(marks.field("status")).containsExactly("EXCUSED");
        assertThat(diary.field("score")).containsExactly(9);

        // И обратное сочетание: присутствовал, но оценки нет.
        put(asTeacher, List.of(new AttendanceEntryRequest(otherPupil.getId(), AttendanceStatus.PRESENT)));
        ItemsPage journal = asTeacher.get().uri("/api/v1/lessons/" + lessonId + "/grades")
                .exchange().returnResult(ItemsPage.class).getResponseBody();
        assertThat(journal.items()).anySatisfy(entry -> {
            assertThat(entry.get("fullName")).isEqualTo("Бекзат Амиров");
            assertThat(entry.get("score")).isNull();
        });
    }

    @Test
    @DisplayName("повторная отправка того же статуса ничего не меняет")
    void putIsIdempotent() {
        var body = List.of(new AttendanceEntryRequest(pupil.getId(), AttendanceStatus.PRESENT));
        put(asTeacher, body);
        var first = attendance.findByLessonIdAndStudentId(lessonId, pupil.getId()).orElseThrow();

        put(asTeacher, body);

        assertThat(attendance.count()).isEqualTo(1);
        assertThat(attendance.findByLessonIdAndStudentId(lessonId, pupil.getId()).orElseThrow()
                .getUpdatedAt()).isEqualTo(first.getUpdatedAt());
    }

    @Test
    @DisplayName("статус переставляется: прогул можно исправить на уважительную причину")
    void statusCanBeCorrected() {
        put(asTeacher, List.of(new AttendanceEntryRequest(pupil.getId(), AttendanceStatus.UNEXCUSED)));
        put(asTeacher, List.of(new AttendanceEntryRequest(pupil.getId(), AttendanceStatus.EXCUSED)));

        assertThat(attendance.count()).isEqualTo(1);
        assertThat(attendance.findByLessonIdAndStudentId(lessonId, pupil.getId()).orElseThrow()
                .getStatus()).isEqualTo(AttendanceStatus.EXCUSED);
    }

    @Test
    @DisplayName("ведомость показывает состав класса на дату урока, неотмеченный — null")
    void sheetListsRosterWithGaps() {
        put(asTeacher, List.of(new AttendanceEntryRequest(pupil.getId(), AttendanceStatus.LATE)));

        ItemsPage sheet = asTeacher.get().uri("/api/v1/lessons/" + lessonId + "/attendance")
                .exchange().returnResult(ItemsPage.class).getResponseBody();

        assertThat(sheet.total()).isEqualTo(2);
        assertThat(sheet.field("fullName")).containsExactly("Асель Нурланова", "Бекзат Амиров");
        assertThat(sheet.field("status")).containsExactly("LATE", null);
    }

    @Test
    @DisplayName("ученика чужого класса отметить нельзя — 422 STUDENT_NOT_ENROLLED")
    void cannotMarkStudentWhoWasNotEnrolled() {
        User outsider = createUser("outsider@school.kz", Role.STUDENT);

        EntityExchangeResult<ErrorEnvelope> result = putExpectingError(asTeacher, List.of(
                new AttendanceEntryRequest(pupil.getId(), AttendanceStatus.PRESENT),
                new AttendanceEntryRequest(outsider.getId(), AttendanceStatus.PRESENT)));

        assertThat(result.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(result.getResponseBody().error().code()).isEqualTo("STUDENT_NOT_ENROLLED");
        assertThat(attendance.count()).as("ведомость применяется целиком или никак").isZero();
    }

    @Test
    @DisplayName("дубликат ученика в теле — 400 VALIDATION_FAILED")
    void duplicateStudentRejected() {
        EntityExchangeResult<ErrorEnvelope> result = putExpectingError(asTeacher, List.of(
                new AttendanceEntryRequest(pupil.getId(), AttendanceStatus.PRESENT),
                new AttendanceEntryRequest(pupil.getId(), AttendanceStatus.UNEXCUSED)));

        assertThat(result.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(result.getResponseBody().error().code()).isEqualTo("VALIDATION_FAILED");
    }

    @Test
    @DisplayName("завуч читает ведомость, но не ведёт её")
    void adminReadsButDoesNotMark() {
        put(asTeacher, List.of(new AttendanceEntryRequest(pupil.getId(), AttendanceStatus.PRESENT)));
        RestTestClient admin = as("zavuch@school.kz", Role.ADMIN);

        assertThat(admin.get().uri("/api/v1/lessons/" + lessonId + "/attendance")
                .exchange().returnResult(ItemsPage.class).getResponseBody().total()).isEqualTo(2);

        EntityExchangeResult<ErrorEnvelope> write = putExpectingError(admin,
                List.of(new AttendanceEntryRequest(pupil.getId(), AttendanceStatus.UNEXCUSED)));
        assertThat(write.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(write.getResponseBody().error().code()).isEqualTo("ROLE_FORBIDDEN");
    }

    @Test
    @DisplayName("учитель не ведёт и не читает ведомость чужого класса")
    void teacherCannotTouchForeignSheet() {
        createUser("colleague@school.kz", Role.TEACHER);
        RestTestClient asColleague = authorized(login("colleague@school.kz").accessToken());

        assertThat(putExpectingError(asColleague,
                List.of(new AttendanceEntryRequest(pupil.getId(), AttendanceStatus.PRESENT)))
                .getResponseBody().error().code()).isEqualTo("NOT_OWNER");

        assertThat(asColleague.get().uri("/api/v1/lessons/" + lessonId + "/attendance")
                .exchange().returnResult(ErrorEnvelope.class)
                .getResponseBody().error().code()).isEqualTo("NOT_OWNER");
    }

    @Test
    @DisplayName("ученик читает свою посещаемость, чужую — 403 NOT_OWNER")
    void studentReadsOwnAttendanceOnly() {
        put(asTeacher, List.of(
                new AttendanceEntryRequest(pupil.getId(), AttendanceStatus.UNEXCUSED),
                new AttendanceEntryRequest(otherPupil.getId(), AttendanceStatus.PRESENT)));
        RestTestClient asPupil = authorized(login("pupil@school.kz").accessToken());

        assertThat(asPupil.get().uri("/api/v1/students/" + pupil.getId() + "/attendance")
                .exchange().returnResult(ItemsPage.class).getResponseBody().field("status"))
                .containsExactly("UNEXCUSED");

        EntityExchangeResult<ErrorEnvelope> denied = asPupil.get()
                .uri("/api/v1/students/" + otherPupil.getId() + "/attendance")
                .exchange().returnResult(ErrorEnvelope.class);
        assertThat(denied.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(denied.getResponseBody().error().code()).isEqualTo("NOT_OWNER");
    }

    @Test
    @DisplayName("ученику ведомость урока не показывается вовсе — 403")
    void studentCannotReadSheet() {
        RestTestClient asPupil = authorized(login("pupil@school.kz").accessToken());

        EntityExchangeResult<ErrorEnvelope> result = asPupil.get()
                .uri("/api/v1/lessons/" + lessonId + "/attendance")
                .exchange().returnResult(ErrorEnvelope.class);

        assertThat(result.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(result.getResponseBody().error().code()).isEqualTo("ROLE_FORBIDDEN");
    }

    @Test
    @DisplayName("сводка ученика фильтруется по четверти")
    void summaryFiltersByTerm() {
        short second = createTerm(academicYear, 2, "2025-11-03", "2025-12-28");
        long novemberLesson = createLesson(assignment, second, "Проценты", "2025-11-10");

        put(asTeacher, List.of(new AttendanceEntryRequest(pupil.getId(), AttendanceStatus.PRESENT)));
        asTeacher.put().uri("/api/v1/lessons/" + novemberLesson + "/attendance")
                .body(List.of(new AttendanceEntryRequest(pupil.getId(), AttendanceStatus.LATE)))
                .exchange().returnResult(String.class);

        String base = "/api/v1/students/" + pupil.getId() + "/attendance";
        assertThat(summary(base).total()).isEqualTo(2);
        assertThat(summary(base + "?termId=" + second).field("lessonTitle"))
                .containsExactly("Проценты");
    }

    @Test
    @DisplayName("удаление урока уносит и его отметки посещаемости")
    void deletingLessonRemovesAttendance() {
        put(asTeacher, List.of(new AttendanceEntryRequest(pupil.getId(), AttendanceStatus.PRESENT)));

        assertThat(asTeacher.delete().uri("/api/v1/lessons/" + lessonId)
                .exchange().returnResult(Void.class).getStatus()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(attendance.count()).isZero();
    }

    @Test
    @DisplayName("посещаемость закрыта без токена")
    void attendanceRequiresToken() {
        for (String uri : new String[]{"/api/v1/lessons/" + lessonId + "/attendance",
                "/api/v1/students/" + pupil.getId() + "/attendance"}) {
            assertThat(client.get().uri(uri).exchange().returnResult(String.class).getStatus())
                    .as(uri).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    private ItemsPage summary(String uri) {
        return asTeacher.get().uri(uri).exchange().returnResult(ItemsPage.class).getResponseBody();
    }
}
