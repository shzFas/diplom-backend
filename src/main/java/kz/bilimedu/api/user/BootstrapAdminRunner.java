package kz.bilimedu.api.user;

import kz.bilimedu.api.config.BootstrapAdminProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Заводит первого администратора, чтобы систему было чем начать наполнять.
 *
 * <p>Ничего не делает, если активный ADMIN уже есть: повторный запуск
 * с теми же переменными не сбрасывает пароль работающему администратору
 * и не плодит дубликаты.
 */
@Component
public class BootstrapAdminRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapAdminRunner.class);

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final BootstrapAdminProperties properties;

    public BootstrapAdminRunner(UserRepository users,
                                PasswordEncoder passwordEncoder,
                                BootstrapAdminProperties properties) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (users.existsByRoleAndDeactivatedAtIsNull(Role.ADMIN)) {
            return;
        }
        if (!properties.configured()) {
            log.warn("Активного администратора нет, а BOOTSTRAP_ADMIN_EMAIL и BOOTSTRAP_ADMIN_PASSWORD "
                    + "не заданы: завести пользователей будет некому");
            return;
        }
        if (users.emailTaken(properties.email())) {
            log.warn("Сид администратора пропущен: email {} уже занят", properties.email());
            return;
        }

        User admin = users.save(new User(
                properties.fullNameOrDefault(),
                properties.email(),
                passwordEncoder.encode(properties.password()),
                Role.ADMIN));

        log.info("Создан первый администратор: {} (id {}). Уберите BOOTSTRAP_ADMIN_* из окружения "
                + "и смените пароль через POST /api/v1/me/password", admin.getEmail(), admin.getId());
    }
}
