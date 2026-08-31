package kz.bilimedu.api.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Настройки токенов. Секрет обязателен и берётся из окружения:
 * приложение падает на старте, если JWT_SECRET не задан.
 *
 * <p>В версии 2023 года секрет был строкой "secret123", захардкоженной
 * в двух файлах — подписать себе токен администратора мог кто угодно.
 */
@Validated
@ConfigurationProperties(prefix = "bilimedu.jwt")
public record JwtProperties(

        @NotBlank
        @Size(min = 32, message = "JWT_SECRET должен быть не короче 32 символов")
        String secret,

        @NotBlank String issuer,

        @NotNull Duration accessTtl,

        @NotNull Duration refreshTtl) {
}
