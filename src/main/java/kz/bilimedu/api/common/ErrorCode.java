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

    /** Дополнение к контракту: email занят другим пользователем. */
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT),
    /** Дополнение к контракту: администратор пытается деактивировать сам себя. */
    CANNOT_DEACTIVATE_SELF(HttpStatus.CONFLICT),

    /** Дополнение к контракту: учебный год с таким названием уже есть. */
    ACADEMIC_YEAR_ALREADY_EXISTS(HttpStatus.CONFLICT),
    /** Правило D2: четверти внутри года не пересекаются. */
    TERM_OVERLAPS(HttpStatus.CONFLICT),
    /** Правило D1: номер четверти в году уникален и лежит в 1..4. */
    TERM_ORDINAL_TAKEN(HttpStatus.CONFLICT),
    /** Дополнение к контракту: класс с таким именем в этом году уже есть. */
    CLASS_ALREADY_EXISTS(HttpStatus.CONFLICT),
    /** Дополнение к контракту: предмет с таким названием уже есть. */
    SUBJECT_ALREADY_EXISTS(HttpStatus.CONFLICT),
    /** Дополнение к контракту: у пары «класс + предмет» уже есть учитель. */
    ASSIGNMENT_ALREADY_EXISTS(HttpStatus.CONFLICT),
    /** Дополнение к контракту: у назначения есть уроки, удаление унесло бы журнал. */
    ASSIGNMENT_HAS_LESSONS(HttpStatus.CONFLICT),
    /** Дополнение к контракту: дубль урока в назначении на ту же дату. */
    LESSON_ALREADY_EXISTS(HttpStatus.CONFLICT),

    /** Дополнение к контракту: перевод оформляют только по активному зачислению. */
    ENROLLMENT_NOT_ACTIVE(HttpStatus.CONFLICT),
    /** Дополнение к контракту: перевод в тот же класс, где ученик и так числится. */
    ALREADY_IN_CLASS(HttpStatus.CONFLICT),

    SOCH_ALREADY_EXISTS(HttpStatus.CONFLICT),
    GRADE_ALREADY_EXISTS(HttpStatus.CONFLICT),
    ENROLLMENT_OVERLAPS(HttpStatus.CONFLICT),
    TERM_CLOSED(HttpStatus.CONFLICT),

    /** Дополнение к контракту: на предмет назначают только пользователя с ролью TEACHER. */
    NOT_A_TEACHER(HttpStatus.UNPROCESSABLE_CONTENT),
    /** Дополнение к контракту: зачислить можно только пользователя с ролью STUDENT. */
    NOT_A_STUDENT(HttpStatus.UNPROCESSABLE_CONTENT),
    /**
     * Дополнение к контракту: четверть принадлежит другому учебному году,
     * чем класс урока. Схемой это не ловится — lessons ссылается на term
     * и на assignment независимо, а триггер проверяет только даты.
     */
    TERM_YEAR_MISMATCH(HttpStatus.UNPROCESSABLE_CONTENT),

    GRADE_EXCEEDS_MAX(HttpStatus.UNPROCESSABLE_CONTENT),
    STUDENT_NOT_ENROLLED(HttpStatus.UNPROCESSABLE_CONTENT),
    LESSON_OUTSIDE_TERM(HttpStatus.UNPROCESSABLE_CONTENT),

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
