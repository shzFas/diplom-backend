package kz.bilimedu.api.user;

import static org.assertj.core.api.Assertions.assertThat;

import kz.bilimedu.api.auth.dto.LoginRequest;
import kz.bilimedu.api.auth.dto.TokenPairResponse;
import kz.bilimedu.api.support.TestcontainersConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * Сид первого администратора. Собственный контекст с собственными
 * переменными: сид отрабатывает на старте приложения, поэтому проверять
 * его в общем контексте, где база чистится перед каждым тестом, нечем.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "bilimedu.bootstrap.admin.email=zavuch@school.kz",
                "bilimedu.bootstrap.admin.password=bootstrap-password",
                "bilimedu.bootstrap.admin.full-name=Завуч Школы"
        })
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class BootstrapAdminIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository users;

    @Autowired
    private BootstrapAdminRunner runner;

    @Test
    @DisplayName("на пустой базе создаётся администратор, который сразу входит")
    void seedsAdminOnEmptyDatabase() {
        User admin = users.findByEmail("zavuch@school.kz").orElseThrow();
        assertThat(admin.getRole()).isEqualTo(Role.ADMIN);
        assertThat(admin.getFullName()).isEqualTo("Завуч Школы");
        assertThat(admin.isActive()).isTrue();

        TokenPairResponse tokens = RestTestClient.bindToServer()
                .baseUrl("http://localhost:" + port).build()
                .post().uri("/api/v1/auth/login")
                .body(new LoginRequest("zavuch@school.kz", "bootstrap-password"))
                .exchange()
                .returnResult(TokenPairResponse.class)
                .getResponseBody();

        assertThat(tokens.accessToken()).isNotBlank();
        assertThat(tokens.user().role()).isEqualTo(Role.ADMIN);
    }

    @Test
    @DisplayName("повторный запуск не плодит администраторов и не сбрасывает пароль")
    void doesNothingWhenAdminAlreadyExists() {
        long before = users.count();
        String hashBefore = users.findByEmail("zavuch@school.kz").orElseThrow().getPasswordHash();

        runner.run((ApplicationArguments) null);

        assertThat(users.count()).isEqualTo(before);
        assertThat(users.findByEmail("zavuch@school.kz").orElseThrow().getPasswordHash())
                .isEqualTo(hashBefore);
    }
}
