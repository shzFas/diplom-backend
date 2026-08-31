package kz.bilimedu.api.user;

/**
 * Представление пользователя наружу. passwordHash здесь нет и быть не может:
 * в версии 2023 года getMe отдавал документ целиком вместе с хешем.
 *
 * <p>id — строка: bigint не помещается в double, которым JS представляет числа.
 */
public record UserResponse(String id, String fullName, String email, Role role, String avatarUrl,
                           boolean telegramLinked) {

    public static UserResponse from(User user) {
        return new UserResponse(
                String.valueOf(user.getId()),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.getAvatarUrl(),
                user.getTelegramChatId() != null);
    }
}
