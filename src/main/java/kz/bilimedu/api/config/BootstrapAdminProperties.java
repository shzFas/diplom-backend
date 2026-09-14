package kz.bilimedu.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Первый администратор. Ручки создания пользователей принадлежат ADMIN,
 * а регистрации самозаписью нет — без этого система не запускается вообще.
 *
 * <p>Сид срабатывает ровно один раз: только если в базе нет ни одного
 * активного администратора и обе переменные заданы. В рабочей среде их
 * задают на первый запуск и убирают.
 */
@ConfigurationProperties(prefix = "bilimedu.bootstrap.admin")
public record BootstrapAdminProperties(String email, String password, String fullName) {

    public boolean configured() {
        return email != null && !email.isBlank() && password != null && !password.isBlank();
    }

    public String fullNameOrDefault() {
        return fullName == null || fullName.isBlank() ? "Администратор" : fullName;
    }
}
