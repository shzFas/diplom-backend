package kz.bilimedu.api.user;

/**
 * Роли не наследуют права друг друга: ADMIN — не надмножество TEACHER.
 * В частности, ADMIN не имеет права записи в grades (docs/permissions.md).
 */
public enum Role {
    ADMIN,
    TEACHER,
    STUDENT
}
