package kz.bilimedu.api.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import kz.bilimedu.api.user.Role;

/**
 * Заводит пользователя только ADMIN. Самозаписи нет: в версии 2023 года
 * POST /auth/register был открыт, и любой мог создать себе учительский аккаунт.
 */
public record CreateUserRequest(

        @NotBlank @Size(max = 200) String fullName,

        @NotBlank @Email @Size(max = 320) String email,

        @NotNull Role role,

        @NotBlank
        @Size(min = 10, message = "Пароль должен быть не короче 10 символов")
        String password) {
}
