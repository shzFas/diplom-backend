package kz.bilimedu.api.common;

import java.util.Map;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

/**
 * Переводит нарушение ограничения PostgreSQL в код ошибки контракта.
 *
 * <p>Так задумано в `docs/api-v1.md`: источником истины остаётся база, а не
 * проверка в приложении. Проверку «нет ли уже такой записи» перед вставкой
 * обходят двумя параллельными запросами — именно этим страдала версия
 * 2023 года, где дубликаты искались через `.filter()` по загруженной
 * в память Node коллекции.
 *
 * <p>Имена взяты из схемы, а не угаданы: они проверены запросом
 * к `pg_constraint` на накатанных миграциях.
 */
@Component
public class ConstraintViolationTranslator {

    private static final Map<String, ErrorCode> BY_CONSTRAINT = Map.ofEntries(
            Map.entry("users_email_key", ErrorCode.EMAIL_ALREADY_EXISTS),
            Map.entry("academic_years_name_key", ErrorCode.ACADEMIC_YEAR_ALREADY_EXISTS),
            Map.entry("terms_academic_year_id_daterange_excl", ErrorCode.TERM_OVERLAPS),
            Map.entry("terms_academic_year_id_ordinal_key", ErrorCode.TERM_ORDINAL_TAKEN),
            Map.entry("classes_academic_year_id_name_key", ErrorCode.CLASS_ALREADY_EXISTS),
            Map.entry("subjects_name_key", ErrorCode.SUBJECT_ALREADY_EXISTS),
            Map.entry("teaching_assignments_class_id_subject_id_key", ErrorCode.ASSIGNMENT_ALREADY_EXISTS),
            Map.entry("enrollments_student_id_daterange_excl", ErrorCode.ENROLLMENT_OVERLAPS),
            Map.entry("lessons_assignment_id_lesson_date_title_key", ErrorCode.LESSON_ALREADY_EXISTS),
            Map.entry("lessons_one_soch_per_term_idx", ErrorCode.SOCH_ALREADY_EXISTS),
            Map.entry("grades_lesson_id_student_id_key", ErrorCode.GRADE_ALREADY_EXISTS));

    /**
     * Сообщения триггеров: межтабличные правила нельзя выразить через CHECK,
     * поэтому у них нет имени ограничения — опознаём по тексту, который
     * триггер сам и формирует.
     */
    private static final Map<String, ErrorCode> BY_TRIGGER_MESSAGE = Map.of(
            "is outside term", ErrorCode.LESSON_OUTSIDE_TERM,
            "exceeds max_score", ErrorCode.GRADE_EXCEEDS_MAX);

    public Optional<ErrorCode> translate(DataIntegrityViolationException exception) {
        String text = flatten(exception);
        if (text == null) {
            return Optional.empty();
        }

        for (var entry : BY_CONSTRAINT.entrySet()) {
            if (text.contains(entry.getKey())) {
                return Optional.of(entry.getValue());
            }
        }
        for (var entry : BY_TRIGGER_MESSAGE.entrySet()) {
            if (text.contains(entry.getKey())) {
                return Optional.of(entry.getValue());
            }
        }
        return Optional.empty();
    }

    /** Имя ограничения лежит в сообщении самого глубокого исключения цепочки. */
    private String flatten(Throwable exception) {
        StringBuilder text = new StringBuilder();
        for (Throwable current = exception; current != null; current = current.getCause()) {
            if (current.getMessage() != null) {
                text.append(current.getMessage()).append('\n');
            }
            if (current.getCause() == current) {
                break;
            }
        }
        return text.isEmpty() ? null : text.toString();
    }
}
