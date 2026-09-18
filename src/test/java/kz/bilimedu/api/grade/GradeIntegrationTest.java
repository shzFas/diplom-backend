package kz.bilimedu.api.grade;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import kz.bilimedu.api.grade.dto.DeleteGradeRequest;
import kz.bilimedu.api.grade.dto.GradeEntryRequest;
import kz.bilimedu.api.grade.dto.UpdateGradeRequest;
import kz.bilimedu.api.support.AbstractIntegrationTest;
import kz.bilimedu.api.user.Role;
import kz.bilimedu.api.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.client.EntityExchangeResult;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * Оценки, журнал и аудит. Отправная точка — версия 2023 года, где
 * POST /marks, GET /marksall и DELETE /marks/:studentId/:ktpId отвечали
 * анонимному вызову, а удаление не оставляло следа.
 */
class GradeIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private GradeRepository grades;

    @Autowired
    private GradeAuditRepository audits;

    private User teacher;
    private User pupil;
    private User otherPupil;
    private short academicYear;
    private short term;
    private long schoolClass;
    private long assignment;
    private long lessonId;
    private RestTestClient asTeacher;

    /** Класс с двумя учениками, одним назначением и одним уроком на 15 сентября. */
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

    private EntityExchangeResult<String> putGrades(RestTestClient as, Object body) {
        return as.put().uri("/api/v1/lessons/" + lessonId + "/grades")
                .body(body).exchange().returnResult(String.class);
    }

    private EntityExchangeResult<ErrorEnvelope> putGradesExpectingError(RestTestClient as, Object body) {
        return as.put().uri("/api/v1/lessons/" + lessonId + "/grades")
                .body(body).exchange().returnResult(ErrorEnvelope.class);
    }

    private List<GradeEntryRequest> wholeClass(short first, short second) {
        return List.of(new GradeEntryRequest(pupil.getId(), first),
                new GradeEntryRequest(otherPupil.getId(), second));
    }

    @Test
    @DisplayName("учитель закрывает журнал класса одним запросом, аудит пишет CREATED")
    void teacherGradesWholeClassAtOnce() {
        assertThat(putGrades(asTeacher, wholeClass((short) 8, (short) 10)).getStatus())
                .isEqualTo(HttpStatus.OK);

        assertThat(grades.findByLessonId(lessonId)).hasSize(2)
                .allSatisfy(grade -> assertThat(grade.getGradedBy()).isEqualTo(teacher.getId()));
        assertThat(audits.findAll()).hasSize(2)
                .allSatisfy(audit -> assertThat(audit.getAction()).isEqualTo(GradeAction.CREATED));
    }

    @Test
    @DisplayName("повторная отправка того же тела не меняет ни оценок, ни аудита")
    void putIsStrictlyIdempotent() {
        putGrades(asTeacher, wholeClass((short) 8, (short) 10));
        long auditsAfterFirst = audits.count();

        putGrades(asTeacher, wholeClass((short) 8, (short) 10));

        assertThat(grades.count()).isEqualTo(2);
        assertThat(audits.count()).as("повторное сохранение не должно засорять аудит")
                .isEqualTo(auditsAfterFirst);
    }

    @Test
    @DisplayName("изменённый балл в списке пишет UPDATED со старым и новым значением")
    void changedScoreIsAudited() {
        putGrades(asTeacher, wholeClass((short) 8, (short) 10));
        putGrades(asTeacher, wholeClass((short) 9, (short) 10));

        var updated = audits.findAll().stream()
                .filter(audit -> audit.getAction() == GradeAction.UPDATED)
                .toList();
        assertThat(updated).singleElement().satisfies(audit -> {
            assertThat(audit.getOldScore()).isEqualTo((short) 8);
            assertThat(audit.getNewScore()).isEqualTo((short) 9);
            assertThat(audit.getStudentId()).isEqualTo(pupil.getId());
        });
    }

    /** Правило D7 — триггер grade_within_lesson_max, максимум лежит на уроке. */
    @Test
    @DisplayName("балл выше максимума урока — 422 GRADE_EXCEEDS_MAX")
    void scoreCannotExceedLessonMaximum() {
        EntityExchangeResult<ErrorEnvelope> result =
                putGradesExpectingError(asTeacher, List.of(new GradeEntryRequest(pupil.getId(), (short) 99)));

        assertThat(result.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(result.getResponseBody().error().code()).isEqualTo("GRADE_EXCEEDS_MAX");
        assertThat(grades.count()).isZero();
    }

    /** Правило D9: ученик должен числиться в классе на дату урока. */
    @Test
    @DisplayName("ученику чужого класса оценку поставить нельзя — 422 STUDENT_NOT_ENROLLED")
    void cannotGradeStudentWhoWasNotEnrolled() {
        User outsider = createUser("outsider@school.kz", Role.STUDENT);

        EntityExchangeResult<ErrorEnvelope> result = putGradesExpectingError(asTeacher,
                List.of(new GradeEntryRequest(pupil.getId(), (short) 8),
                        new GradeEntryRequest(outsider.getId(), (short) 9)));

        assertThat(result.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(result.getResponseBody().error().code()).isEqualTo("STUDENT_NOT_ENROLLED");
        assertThat(result.getResponseBody().error().details())
                .anySatisfy(detail -> assertThat(detail).containsEntry("value", String.valueOf(outsider.getId())));
        assertThat(grades.count()).as("журнал применяется целиком или никак").isZero();
    }

    @Test
    @DisplayName("дубликат ученика в теле — 400 VALIDATION_FAILED")
    void duplicateStudentInBodyRejected() {
        EntityExchangeResult<ErrorEnvelope> result = putGradesExpectingError(asTeacher,
                List.of(new GradeEntryRequest(pupil.getId(), (short) 8),
                        new GradeEntryRequest(pupil.getId(), (short) 9)));

        assertThat(result.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(result.getResponseBody().error().code()).isEqualTo("VALIDATION_FAILED");
    }

    /** Правило D4: закрытая четверть не принимает новые и изменённые оценки. */
    @Test
    @DisplayName("в закрытой четверти оценки не ставятся, не правятся и не удаляются")
    void closedTermRejectsEveryChange() {
        putGrades(asTeacher, wholeClass((short) 8, (short) 10));
        long gradeId = grades.findByLessonId(lessonId).getFirst().getId();

        RestTestClient admin = as("zavuch@school.kz", Role.ADMIN);
        admin.post().uri("/api/v1/terms/" + term + "/close").exchange().returnResult(String.class);

        assertThat(putGradesExpectingError(asTeacher, wholeClass((short) 7, (short) 7))
                .getResponseBody().error().code()).isEqualTo("TERM_CLOSED");

        assertThat(asTeacher.patch().uri("/api/v1/grades/" + gradeId)
                .body(new UpdateGradeRequest((short) 5, "пересмотр"))
                .exchange().returnResult(ErrorEnvelope.class)
                .getResponseBody().error().code()).isEqualTo("TERM_CLOSED");

        assertThat(asTeacher.method(HttpMethod.DELETE).uri("/api/v1/grades/" + gradeId)
                .body(new DeleteGradeRequest("ошибка"))
                .exchange().returnResult(ErrorEnvelope.class)
                .getResponseBody().error().code()).isEqualTo("TERM_CLOSED");

        // После переоткрытия правка снова проходит: закрытие обратимо.
        admin.post().uri("/api/v1/terms/" + term + "/reopen").exchange().returnResult(String.class);
        assertThat(putGrades(asTeacher, wholeClass((short) 7, (short) 7)).getStatus())
                .isEqualTo(HttpStatus.OK);
    }

    /** Приёмочный критерий: ADMIN получает 403 на выставление оценок. */
    @Test
    @DisplayName("завуч не выставляет оценки — иначе аудит потерял бы смысл")
    void adminCannotGrade() {
        EntityExchangeResult<ErrorEnvelope> result =
                putGradesExpectingError(as("zavuch@school.kz", Role.ADMIN), wholeClass((short) 8, (short) 10));

        assertThat(result.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(result.getResponseBody().error().code()).isEqualTo("ROLE_FORBIDDEN");
        assertThat(grades.count()).isZero();
    }

    /** Приёмочный критерий: токен ученика получает 403 на выставление оценок. */
    @Test
    @DisplayName("ученик не выставляет оценки")
    void studentCannotGrade() {
        RestTestClient asPupil = authorized(login("pupil@school.kz").accessToken());

        EntityExchangeResult<ErrorEnvelope> result = putGradesExpectingError(asPupil, wholeClass((short) 10, (short) 10));

        assertThat(result.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(result.getResponseBody().error().code()).isEqualTo("ROLE_FORBIDDEN");
    }

    @Test
    @DisplayName("учитель не ставит оценки в чужом назначении — 403 NOT_OWNER")
    void teacherCannotGradeForeignAssignment() {
        User colleague = createUser("colleague@school.kz", Role.TEACHER);
        RestTestClient asColleague = authorized(login("colleague@school.kz").accessToken());

        EntityExchangeResult<ErrorEnvelope> result =
                putGradesExpectingError(asColleague, wholeClass((short) 8, (short) 10));

        assertThat(result.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(result.getResponseBody().error().code()).isEqualTo("NOT_OWNER");
        assertThat(colleague.getId()).isNotEqualTo(teacher.getId());
    }

    /** Приёмочный критерий: учитель получает 403 на журнал чужого класса. */
    @Test
    @DisplayName("учитель не видит журнал чужого класса")
    void teacherCannotReadForeignJournal() {
        createUser("colleague@school.kz", Role.TEACHER);
        RestTestClient asColleague = authorized(login("colleague@school.kz").accessToken());

        EntityExchangeResult<ErrorEnvelope> result = asColleague.get()
                .uri("/api/v1/lessons/" + lessonId + "/grades")
                .exchange().returnResult(ErrorEnvelope.class);

        assertThat(result.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(result.getResponseBody().error().code()).isEqualTo("NOT_OWNER");
    }

    @Test
    @DisplayName("ученику журнал урока не показывается вовсе — 403 ROLE_FORBIDDEN")
    void studentCannotReadJournal() {
        RestTestClient asPupil = authorized(login("pupil@school.kz").accessToken());

        EntityExchangeResult<ErrorEnvelope> result = asPupil.get()
                .uri("/api/v1/lessons/" + lessonId + "/grades")
                .exchange().returnResult(ErrorEnvelope.class);

        assertThat(result.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(result.getResponseBody().error().code()).isEqualTo("ROLE_FORBIDDEN");
    }

    /** Приёмочный критерий: ученик A получает 403 на оценки ученика B. */
    @Test
    @DisplayName("ученик читает только свои оценки")
    void studentReadsOwnGradesOnly() {
        putGrades(asTeacher, wholeClass((short) 8, (short) 10));
        RestTestClient asPupil = authorized(login("pupil@school.kz").accessToken());

        ItemsPage own = asPupil.get().uri("/api/v1/students/" + pupil.getId() + "/grades")
                .exchange().returnResult(ItemsPage.class).getResponseBody();
        assertThat(own.total()).isEqualTo(1);
        assertThat(own.field("score")).containsExactly(8);

        EntityExchangeResult<ErrorEnvelope> denied = asPupil.get()
                .uri("/api/v1/students/" + otherPupil.getId() + "/grades")
                .exchange().returnResult(ErrorEnvelope.class);
        assertThat(denied.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(denied.getResponseBody().error().code()).isEqualTo("NOT_OWNER");
    }

    @Test
    @DisplayName("журнал показывает состав класса на дату урока, без оценки — null")
    void journalListsRosterWithGaps() {
        putGrades(asTeacher, List.of(new GradeEntryRequest(pupil.getId(), (short) 8)));

        ItemsPage journal = asTeacher.get().uri("/api/v1/lessons/" + lessonId + "/grades")
                .exchange().returnResult(ItemsPage.class).getResponseBody();

        assertThat(journal.total()).isEqualTo(2);
        assertThat(journal.field("fullName")).containsExactly("Асель Нурланова", "Бекзат Амиров");
        assertThat(journal.field("score")).containsExactly(8, null);
    }

    @Test
    @DisplayName("PATCH правит балл с причиной; без причины — 400")
    void patchRequiresReason() {
        putGrades(asTeacher, wholeClass((short) 8, (short) 10));
        long gradeId = grades.findByLessonIdAndStudentId(lessonId, pupil.getId()).orElseThrow().getId();

        assertThat(asTeacher.patch().uri("/api/v1/grades/" + gradeId)
                .body(new UpdateGradeRequest((short) 6, "пересчёт после апелляции"))
                .exchange().returnResult(String.class).getStatus()).isEqualTo(HttpStatus.OK);

        assertThat(grades.findById(gradeId).orElseThrow().getScore()).isEqualTo((short) 6);
        assertThat(audits.findAll()).anySatisfy(audit -> {
            assertThat(audit.getAction()).isEqualTo(GradeAction.UPDATED);
            assertThat(audit.getReason()).isEqualTo("пересчёт после апелляции");
        });

        EntityExchangeResult<ErrorEnvelope> noReason = asTeacher.patch().uri("/api/v1/grades/" + gradeId)
                .body(new UpdateGradeRequest((short) 5, "  "))
                .exchange().returnResult(ErrorEnvelope.class);
        assertThat(noReason.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(noReason.getResponseBody().error().code()).isEqualTo("VALIDATION_FAILED");
    }

    /** Правило D17: запись аудита переживает удаление самой оценки. */
    @Test
    @DisplayName("удалённая оценка исчезает, её история — нет")
    void auditOutlivesDeletedGrade() {
        putGrades(asTeacher, wholeClass((short) 8, (short) 10));
        long gradeId = grades.findByLessonIdAndStudentId(lessonId, pupil.getId()).orElseThrow().getId();

        assertThat(asTeacher.method(HttpMethod.DELETE).uri("/api/v1/grades/" + gradeId)
                .body(new DeleteGradeRequest("выставлена не тому ученику"))
                .exchange().returnResult(Void.class).getStatus()).isEqualTo(HttpStatus.NO_CONTENT);

        assertThat(grades.findById(gradeId)).isEmpty();

        ItemsPage history = asTeacher.get().uri("/api/v1/grades/" + gradeId + "/audit")
                .exchange().returnResult(ItemsPage.class).getResponseBody();
        assertThat(history.field("action")).contains("DELETED", "CREATED");
        assertThat(history.items().getFirst()).containsEntry("reason", "выставлена не тому ученику");
    }

    @Test
    @DisplayName("DELETE без причины не проходит")
    void deleteRequiresReason() {
        putGrades(asTeacher, wholeClass((short) 8, (short) 10));
        long gradeId = grades.findByLessonIdAndStudentId(lessonId, pupil.getId()).orElseThrow().getId();

        EntityExchangeResult<ErrorEnvelope> result = asTeacher.method(HttpMethod.DELETE)
                .uri("/api/v1/grades/" + gradeId)
                .body(new DeleteGradeRequest(""))
                .exchange().returnResult(ErrorEnvelope.class);

        assertThat(result.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(grades.findById(gradeId)).isPresent();
    }

    @Test
    @DisplayName("дневник ученика фильтруется по четверти и предмету")
    void studentDiaryFilters() {
        short second = createTerm(academicYear, 2, "2025-11-03", "2025-12-28");
        long physics = assignTeacher(teacher.getId(), schoolClass, "Физика");
        long novemberLesson = createLesson(assignment, second, "Проценты", "2025-11-10");
        long physicsLesson = createLesson(physics, term, "Оптика", "2025-09-16");

        putGrades(asTeacher, List.of(new GradeEntryRequest(pupil.getId(), (short) 8)));
        asTeacher.put().uri("/api/v1/lessons/" + novemberLesson + "/grades")
                .body(List.of(new GradeEntryRequest(pupil.getId(), (short) 9)))
                .exchange().returnResult(String.class);
        asTeacher.put().uri("/api/v1/lessons/" + physicsLesson + "/grades")
                .body(List.of(new GradeEntryRequest(pupil.getId(), (short) 7)))
                .exchange().returnResult(String.class);

        String base = "/api/v1/students/" + pupil.getId() + "/grades";
        assertThat(diary(base).total()).isEqualTo(3);
        assertThat(diary(base + "?termId=" + term).field("lessonTitle"))
                .containsExactlyInAnyOrder("Дроби", "Оптика");
        assertThat(diary(base + "?subjectId=" + subjectIdOf(physics)).field("lessonTitle"))
                .containsExactly("Оптика");
    }

    @Test
    @DisplayName("учитель читает оценки своих учеников и не читает чужих")
    void teacherReadsOwnStudentsGrades() {
        putGrades(asTeacher, wholeClass((short) 8, (short) 10));
        User outsider = createUser("outsider@school.kz", Role.STUDENT);

        assertThat(asTeacher.get().uri("/api/v1/students/" + pupil.getId() + "/grades")
                .exchange().returnResult(String.class).getStatus()).isEqualTo(HttpStatus.OK);
        assertThat(asTeacher.get().uri("/api/v1/students/" + outsider.getId() + "/grades")
                .exchange().returnResult(String.class).getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("оценки закрыты без токена")
    void gradesRequireToken() {
        for (String uri : new String[]{"/api/v1/lessons/" + lessonId + "/grades",
                "/api/v1/students/" + pupil.getId() + "/grades",
                "/api/v1/grades/1/audit"}) {
            assertThat(client.get().uri(uri).exchange().returnResult(String.class).getStatus())
                    .as(uri).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    private ItemsPage diary(String uri) {
        return asTeacher.get().uri(uri).exchange().returnResult(ItemsPage.class).getResponseBody();
    }

    private long subjectIdOf(long assignmentId) {
        return jdbc.sql("SELECT subject_id FROM teaching_assignments WHERE id = :id")
                .param("id", assignmentId).query(Long.class).single();
    }
}
