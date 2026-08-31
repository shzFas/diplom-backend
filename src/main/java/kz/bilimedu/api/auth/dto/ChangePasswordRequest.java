package kz.bilimedu.api.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * id пользователя берётся из токена, а не из пути: в версии 2023 года
 * ручка называлась /auth/change-password/teacher/:id и меняла пароль любому.
 *
 * <p>Поле контракта называется "new" — это ключевое слово Java, поэтому
 * компонент записи назван иначе, а имя в JSON задано явно.
 */
public record ChangePasswordRequest(

        @NotBlank String current,

        @JsonProperty("new")
        @NotBlank
        @Size(min = 10, message = "Пароль должен быть не короче 10 символов")
        String updated) {
}
