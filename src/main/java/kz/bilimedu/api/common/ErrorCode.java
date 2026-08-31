package kz.bilimedu.api.common;

import org.springframework.http.HttpStatus;

/**
 * Машинные коды ошибок из docs/api-v1.md. Клиент ветвится по коду,
 * а не по тексту сообщения: текст локализуется и может меняться.
 *
 * <p>Коды, которых нет в исходной таблице контракта, помечены ниже — они
 * закрывают случаи аутентификации, которые контракт не перечислял.
 */
public enum ErrorCode {

    VALIDATION_FAILED(HttpStatus.BAD_REQUEST),

    TOKEN_MISSING(HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED),
    /** Дополнение к контракту: подпись не сходится или токен повреждён. */
    TOKEN_INVALID(HttpStatus.UNAUTHORIZED),
    /** Дополнение к контракту: неверная пара email + пароль. */
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED),
    /** Дополнение к контракту: refresh-токен отозван, истёк или неизвестен. */
    REFRESH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED),

    ROLE_FORBIDDEN(HttpStatus.FORBIDDEN),
    NOT_OWNER(HttpStatus.FORBIDDEN),
    /** Дополнение к контракту: учётная запись деактивирована (users.deactivated_at). */
    ACCOUNT_DEACTIVATED(HttpStatus.FORBIDDEN),

    NOT_FOUND(HttpStatus.NOT_FOUND),

    SOCH_ALREADY_EXISTS(HttpStatus.CONFLICT),
    GRADE_ALREADY_EXISTS(HttpStatus.CONFLICT),
    ENROLLMENT_OVERLAPS(HttpStatus.CONFLICT),
    TERM_CLOSED(HttpStatus.CONFLICT),

    GRADE_EXCEEDS_MAX(HttpStatus.UNPROCESSABLE_ENTITY),
    STUDENT_NOT_ENROLLED(HttpStatus.UNPROCESSABLE_ENTITY),
    LESSON_OUTSIDE_TERM(HttpStatus.UNPROCESSABLE_ENTITY),

    /** Дополнение к контракту: непредвиденная ошибка сервера. */
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

    private final HttpStatus status;

    ErrorCode(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }

    /** Ключ в messages_*.properties. */
    public String messageKey() {
        return "error." + name();
    }
}
