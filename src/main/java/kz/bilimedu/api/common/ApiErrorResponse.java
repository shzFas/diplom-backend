package kz.bilimedu.api.common;

import java.util.List;
import java.util.Map;

/**
 * Единственный формат ошибки на все случаи (docs/api-v1.md).
 * В версии 2023 года их было три: {message}, массив express-validator
 * и голая строка — клиенту приходилось угадывать.
 */
public record ApiErrorResponse(Body error) {

    public record Body(String code, String message, List<Map<String, Object>> details) {
    }

    public static ApiErrorResponse of(ErrorCode code, String message, List<Map<String, Object>> details) {
        return new ApiErrorResponse(new Body(code.name(), message, details));
    }
}
