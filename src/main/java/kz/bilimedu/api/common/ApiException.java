package kz.bilimedu.api.common;

import java.util.List;
import java.util.Map;

/**
 * Ошибка домена или доступа, у которой есть код из контракта.
 * Текст берётся из MessageSource по {@link ErrorCode#messageKey()},
 * поэтому сообщение здесь не хранится — только аргументы подстановки.
 */
public class ApiException extends RuntimeException {

    private final ErrorCode code;
    private final transient Object[] messageArgs;
    private final transient List<Map<String, Object>> details;

    public ApiException(ErrorCode code, Object... messageArgs) {
        this(code, List.of(), messageArgs);
    }

    public ApiException(ErrorCode code, List<Map<String, Object>> details, Object... messageArgs) {
        super(code.name());
        this.code = code;
        this.details = details;
        this.messageArgs = messageArgs;
    }

    public ErrorCode code() {
        return code;
    }

    public Object[] messageArgs() {
        return messageArgs;
    }

    public List<Map<String, Object>> details() {
        return details;
    }
}
