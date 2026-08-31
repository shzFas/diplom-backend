package kz.bilimedu.api.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Одна ручка входа на все роли: было /auth/login и /auth/loginStudent. */
public record LoginRequest(
        @NotBlank @Email String email,
        @NotBlank String password) {
}
