package kz.bilimedu.api.user;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import kz.bilimedu.api.auth.dto.RefreshRequest;
import kz.bilimedu.api.auth.dto.TokenPairResponse;
import kz.bilimedu.api.support.AbstractIntegrationTest;
import kz.bilimedu.api.user.dto.CreateUserRequest;
import kz.bilimedu.api.user.dto.UpdateUserRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.client.EntityExchangeResult;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * Заменяет GET /teacher, GET /students, PUT /teacher/:id и DELETE /student/:id —
 * ручки, которые в версии 2023 года отвечали кому угодно.
 */
class UsersModuleIntegrationTest extends AbstractIntegrationTest {

    private static final CreateUserRequest NEW_TEACHER = new CreateUserRequest(
            "Айгуль Смагулова", "aigul@school.kz", Role.TEACHER, "teacher-password");

    @Test
    @DisplayName("ADMIN заводит пользователя: 201 и профиль без хеша пароля")
    void adminCreatesUser() {
        RestTestClient admin = as("admin@school.kz", Role.ADMIN);

        EntityExchangeResult<String> result = admin.post().uri("/api/v1/users")
                .body(NEW_TEACHER).exchange().returnResult(String.class);

        assertThat(result.getStatus()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getResponseBody())
                .contains("aigul@school.kz")
                .doesNotContain("passwordHash")
                .doesNotContain("$argon2");
        assertThat(users.findByEmail("aigul@school.kz")).isPresent();
    }

    @Test
    @DisplayName("заведённый пользователь сразу может войти")
    void createdUserCanLogIn() {
        as("admin@school.kz", Role.ADMIN).post().uri("/api/v1/users")
                .body(NEW_TEACHER).exchange().returnResult(String.class);

        TokenPairResponse tokens = client.post().uri("/api/v1/auth/login")
                .body(Map.of("email", "aigul@school.kz", "password", "teacher-password"))
                .exchange().returnResult(TokenPairResponse.class).getResponseBody();

        assertThat(tokens.accessToken()).isNotBlank();
        assertThat(tokens.user().role()).isEqualTo(Role.TEACHER);
    }

    @Test
    @DisplayName("занятый email — 409 EMAIL_ALREADY_EXISTS, регистр не спасает")
    void duplicateEmailRejected() {
        RestTestClient admin = as("admin@school.kz", Role.ADMIN);
        admin.post().uri("/api/v1/users").body(NEW_TEACHER).exchange().returnResult(String.class);

        EntityExchangeResult<ErrorEnvelope> result = admin.post().uri("/api/v1/users")
                .body(new CreateUserRequest("Другой Человек", "AIGUL@School.KZ", Role.STUDENT, "another-password"))
                .exchange().returnResult(ErrorEnvelope.class);

        assertThat(result.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(result.getResponseBody().error().code()).isEqualTo("EMAIL_ALREADY_EXISTS");
    }

    @Test
    @DisplayName("ученик не может заводить пользователей: 403 ROLE_FORBIDDEN")
    void studentCannotCreateUsers() {
        EntityExchangeResult<ErrorEnvelope> result = as("pupil@school.kz", Role.STUDENT)
                .post().uri("/api/v1/users").body(NEW_TEACHER)
                .exchange().returnResult(ErrorEnvelope.class);

        assertThat(result.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(result.getResponseBody().error().code()).isEqualTo("ROLE_FORBIDDEN");
    }

    @Test
    @DisplayName("учитель не может заводить пользователей — это право завуча")
    void teacherCannotCreateUsers() {
        EntityExchangeResult<ErrorEnvelope> result = as("teacher@school.kz", Role.TEACHER)
                .post().uri("/api/v1/users").body(NEW_TEACHER)
                .exchange().returnResult(ErrorEnvelope.class);

        assertThat(result.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("ученик читает только себя: чужой профиль — 403 NOT_OWNER")
    void studentReadsOnlySelf() {
        User other = createUser("other@school.kz", Role.STUDENT);
        User self = createUser("pupil@school.kz", Role.STUDENT);
        RestTestClient pupil = authorized(login("pupil@school.kz").accessToken());

        assertThat(pupil.get().uri("/api/v1/users/" + self.getId())
                .exchange().returnResult(String.class).getStatus()).isEqualTo(HttpStatus.OK);

        EntityExchangeResult<ErrorEnvelope> forbidden = pupil.get().uri("/api/v1/users/" + other.getId())
                .exchange().returnResult(ErrorEnvelope.class);
        assertThat(forbidden.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(forbidden.getResponseBody().error().code()).isEqualTo("NOT_OWNER");
    }

    @Test
    @DisplayName("в списке ученик видит ровно одну запись — себя")
    void studentListContainsOnlySelf() {
        createUser("other@school.kz", Role.STUDENT);
        createUser("teacher@school.kz", Role.TEACHER);
        RestTestClient pupil = as("pupil@school.kz", Role.STUDENT);

        UserPage page = pupil.get().uri("/api/v1/users").exchange().returnResult(UserPage.class)
                .getResponseBody();

        assertThat(page.total()).isEqualTo(1);
        assertThat(page.items()).singleElement()
                .extracting(item -> item.get("email")).isEqualTo("pupil@school.kz");
    }

    @Test
    @DisplayName("учитель видит своих учеников и коллег, но не чужих учеников")
    void teacherSeesOwnStudentsAndColleagues() {
        User teacher = createUser("teacher@school.kz", Role.TEACHER, "Свой Учитель");
        createUser("colleague@school.kz", Role.TEACHER, "Коллега");
        User mine = createUser("mine@school.kz", Role.STUDENT, "Мой Ученик");
        User foreign = createUser("foreign@school.kz", Role.STUDENT, "Чужой Ученик");

        long ownClass = createClass("8А");
        long otherClass = createClass("9Б");
        assignTeacher(teacher.getId(), ownClass, "Математика");
        enroll(mine.getId(), ownClass);
        enroll(foreign.getId(), otherClass);

        RestTestClient asTeacher = authorized(login("teacher@school.kz").accessToken());
        UserPage page = asTeacher.get().uri("/api/v1/users").exchange()
                .returnResult(UserPage.class).getResponseBody();

        assertThat(page.items()).extracting(item -> item.get("email"))
                .contains("teacher@school.kz", "colleague@school.kz", "mine@school.kz")
                .doesNotContain("foreign@school.kz");

        assertThat(asTeacher.get().uri("/api/v1/users/" + foreign.getId())
                .exchange().returnResult(String.class).getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(asTeacher.get().uri("/api/v1/users/" + mine.getId())
                .exchange().returnResult(String.class).getStatus()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("фильтры role и classId сужают список")
    void listFiltersByRoleAndClass() {
        RestTestClient admin = as("admin@school.kz", Role.ADMIN);
        User inClass = createUser("inclass@school.kz", Role.STUDENT);
        createUser("noclass@school.kz", Role.STUDENT);
        long classId = createClass("8А");
        enroll(inClass.getId(), classId);

        UserPage students = admin.get().uri("/api/v1/users?role=STUDENT").exchange()
                .returnResult(UserPage.class).getResponseBody();
        assertThat(students.total()).isEqualTo(2);

        UserPage byClass = admin.get().uri("/api/v1/users?classId=" + classId).exchange()
                .returnResult(UserPage.class).getResponseBody();
        assertThat(byClass.total()).isEqualTo(1);
        assertThat(byClass.items()).singleElement()
                .extracting(item -> item.get("email")).isEqualTo("inclass@school.kz");
    }

    @Test
    @DisplayName("пагинация обязательна и ограничена сверху")
    void listIsPagedAndCapped() {
        RestTestClient admin = as("admin@school.kz", Role.ADMIN);
        for (int i = 0; i < 5; i++) {
            createUser("pupil" + i + "@school.kz", Role.STUDENT, "Ученик " + i);
        }

        UserPage firstPage = admin.get().uri("/api/v1/users?page=0&size=2").exchange()
                .returnResult(UserPage.class).getResponseBody();
        assertThat(firstPage.items()).hasSize(2);
        assertThat(firstPage.size()).isEqualTo(2);
        assertThat(firstPage.total()).isEqualTo(6);

        UserPage capped = admin.get().uri("/api/v1/users?size=100000").exchange()
                .returnResult(UserPage.class).getResponseBody();
        assertThat(capped.size()).isEqualTo(200);
    }

    @Test
    @DisplayName("PATCH меняет имя и email, но не роль")
    void patchUpdatesProfile() {
        RestTestClient admin = as("admin@school.kz", Role.ADMIN);
        User target = createUser("old@school.kz", Role.TEACHER, "Старое Имя");

        EntityExchangeResult<String> result = admin.patch().uri("/api/v1/users/" + target.getId())
                .body(new UpdateUserRequest("Новое Имя", "new@school.kz", null))
                .exchange().returnResult(String.class);

        assertThat(result.getStatus()).isEqualTo(HttpStatus.OK);
        assertThat(result.getResponseBody()).contains("Новое Имя").contains("new@school.kz");

        User reloaded = users.findById(target.getId()).orElseThrow();
        assertThat(reloaded.getRole()).isEqualTo(Role.TEACHER);
    }

    @Test
    @DisplayName("PATCH на занятый email — 409")
    void patchRejectsTakenEmail() {
        RestTestClient admin = as("admin@school.kz", Role.ADMIN);
        createUser("taken@school.kz", Role.TEACHER);
        User target = createUser("free@school.kz", Role.TEACHER);

        EntityExchangeResult<ErrorEnvelope> result = admin.patch().uri("/api/v1/users/" + target.getId())
                .body(new UpdateUserRequest(null, "taken@school.kz", null))
                .exchange().returnResult(ErrorEnvelope.class);

        assertThat(result.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(result.getResponseBody().error().code()).isEqualTo("EMAIL_ALREADY_EXISTS");
    }

    @Test
    @DisplayName("DELETE деактивирует, не удаляя, и немедленно отзывает сессии")
    void deleteDeactivatesAndRevokesSessions() {
        RestTestClient admin = as("admin@school.kz", Role.ADMIN);
        User victim = createUser("victim@school.kz", Role.TEACHER);
        TokenPairResponse victimSession = login("victim@school.kz");

        EntityExchangeResult<Void> result = admin.delete().uri("/api/v1/users/" + victim.getId())
                .exchange().returnResult(Void.class);
        assertThat(result.getStatus()).isEqualTo(HttpStatus.NO_CONTENT);

        assertThat(users.findById(victim.getId())).get()
                .extracting(User::isActive).isEqualTo(false);

        EntityExchangeResult<ErrorEnvelope> refresh = client.post().uri("/api/v1/auth/refresh")
                .body(new RefreshRequest(victimSession.refreshToken()))
                .exchange().returnResult(ErrorEnvelope.class);
        assertThat(refresh.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("деактивированный не попадает в список, но виден с includeDeactivated")
    void deactivatedHiddenByDefault() {
        RestTestClient admin = as("admin@school.kz", Role.ADMIN);
        User victim = createUser("victim@school.kz", Role.TEACHER);
        admin.delete().uri("/api/v1/users/" + victim.getId()).exchange().returnResult(Void.class);

        assertThat(admin.get().uri("/api/v1/users").exchange()
                .returnResult(UserPage.class).getResponseBody().total()).isEqualTo(1);

        assertThat(admin.get().uri("/api/v1/users?includeDeactivated=true").exchange()
                .returnResult(UserPage.class).getResponseBody().total()).isEqualTo(2);
    }

    @Test
    @DisplayName("администратор не может деактивировать сам себя")
    void adminCannotDeactivateSelf() {
        User admin = createUser("admin@school.kz", Role.ADMIN);
        RestTestClient asAdmin = authorized(login("admin@school.kz").accessToken());

        EntityExchangeResult<ErrorEnvelope> result = asAdmin.delete().uri("/api/v1/users/" + admin.getId())
                .exchange().returnResult(ErrorEnvelope.class);

        assertThat(result.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(result.getResponseBody().error().code()).isEqualTo("CANNOT_DEACTIVATE_SELF");
        assertThat(users.findById(admin.getId())).get().extracting(User::isActive).isEqualTo(true);
    }

    @Test
    @DisplayName("несуществующий пользователь — 404")
    void unknownUserIsNotFound() {
        EntityExchangeResult<ErrorEnvelope> result = as("admin@school.kz", Role.ADMIN)
                .get().uri("/api/v1/users/999999").exchange().returnResult(ErrorEnvelope.class);

        assertThat(result.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(result.getResponseBody().error().code()).isEqualTo("NOT_FOUND");
    }

    @Test
    @DisplayName("без токена список пользователей закрыт")
    void listRequiresToken() {
        assertThat(client.get().uri("/api/v1/users").exchange().returnResult(String.class).getStatus())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
