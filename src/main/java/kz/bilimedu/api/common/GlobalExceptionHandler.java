package kz.bilimedu.api.common;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Переводит исключения в единый формат ошибки. Сообщение локализуется
 * по Accept-Language, код остаётся стабильным.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final MessageSource messages;

    public GlobalExceptionHandler(MessageSource messages) {
        this.messages = messages;
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiErrorResponse> handleApi(ApiException ex) {
        return build(ex.code(), ex.details(), ex.messageArgs());
    }

    /** Bean Validation на DTO: поля перечисляются в details. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<Map<String, Object>> details = new ArrayList<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("field", error.getField());
            item.put("message", error.getDefaultMessage());
            details.add(item);
        }
        return build(ErrorCode.VALIDATION_FAILED, details);
    }

    /**
     * Отказ от @PreAuthorize приходит сюда раньше, чем до AccessDeniedHandler
     * Spring Security: исключение бросается внутри вызова контроллера.
     * Без этого обработчика 403 превратился бы в 500.
     */
    @ExceptionHandler({AuthorizationDeniedException.class, AccessDeniedException.class})
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(RuntimeException ex) {
        return build(ErrorCode.ROLE_FORBIDDEN, List.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNoResource(NoResourceFoundException ex) {
        return build(ErrorCode.NOT_FOUND, List.of());
    }

    /**
     * Никакое непредвиденное исключение не должно утечь клиенту стектрейсом:
     * в версии 2023 года ошибки Mongo уходили в тело ответа как есть.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Необработанное исключение на {} {}", request.getMethod(), request.getRequestURI(), ex);
        return build(ErrorCode.INTERNAL_ERROR, List.of());
    }

    private ResponseEntity<ApiErrorResponse> build(ErrorCode code, List<Map<String, Object>> details, Object... args) {
        Locale locale = LocaleContextHolder.getLocale();
        String message = messages.getMessage(code.messageKey(), args, code.name(), locale);
        return ResponseEntity.status(code.status()).body(ApiErrorResponse.of(code, message, details));
    }
}
