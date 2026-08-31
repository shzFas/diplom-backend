package kz.bilimedu.api.security;

import kz.bilimedu.api.user.Role;

/**
 * Principal текущего запроса. Всё, что нужно для авторизации, приходит
 * из подписанного токена — поход в базу на каждый запрос не нужен.
 */
public record AuthenticatedUser(Long id, Role role) {
}
