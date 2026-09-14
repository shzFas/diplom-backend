package kz.bilimedu.api.calendar;

import kz.bilimedu.api.calendar.dto.AcademicYearResponse;
import kz.bilimedu.api.calendar.dto.CreateAcademicYearRequest;
import kz.bilimedu.api.calendar.dto.CreateTermRequest;
import kz.bilimedu.api.calendar.dto.TermResponse;
import kz.bilimedu.api.common.ApiException;
import kz.bilimedu.api.common.ErrorCode;
import kz.bilimedu.api.common.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Учебный календарь. Читают все роли, меняет только завуч.
 *
 * <p>Пересечение четвертей и уникальность номера не проверяются запросом
 * перед вставкой: это делают ограничения базы, а сервис лишь переводит
 * их нарушение в код ответа (см. ConstraintViolationTranslator).
 */
@Service
public class CalendarService {

    private final AcademicYearRepository years;
    private final TermRepository terms;

    public CalendarService(AcademicYearRepository years, TermRepository terms) {
        this.years = years;
        this.terms = terms;
    }

    @Transactional(readOnly = true)
    public PageResponse<AcademicYearResponse> listYears(Pageable pageable) {
        return PageResponse.of(years.findAll(pageable).map(AcademicYearResponse::from));
    }

    @Transactional
    public AcademicYearResponse createYear(CreateAcademicYearRequest request) {
        if (!request.endsOn().isAfter(request.startsOn())) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED);
        }
        AcademicYear created = years.save(
                new AcademicYear(request.name().strip(), request.startsOn(), request.endsOn()));
        return AcademicYearResponse.from(created);
    }

    @Transactional(readOnly = true)
    public PageResponse<TermResponse> listTerms(Short academicYearId, Pageable pageable) {
        var page = academicYearId == null
                ? terms.findAllByOrderByAcademicYearIdAscOrdinalAsc(pageable)
                : terms.findByAcademicYearIdOrderByOrdinalAsc(academicYearId, pageable);
        return PageResponse.of(page.map(TermResponse::from));
    }

    @Transactional
    public TermResponse createTerm(CreateTermRequest request) {
        if (!request.endsOn().isAfter(request.startsOn())) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED);
        }
        AcademicYear year = years.findById(request.academicYearId())
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));

        // Четверть вне своего учебного года сделала бы бессмысленным
        // триггер lesson_date_within_term: урок попадал бы в четверть,
        // лежащую за пределами года.
        if (request.startsOn().isBefore(year.getStartsOn()) || request.endsOn().isAfter(year.getEndsOn())) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED);
        }

        Term created = terms.save(new Term(
                request.academicYearId(), request.ordinal(), request.startsOn(), request.endsOn()));
        return TermResponse.from(created);
    }

    /**
     * Закрытие четверти. Сейчас выставляет только признак, который запрещает
     * правку оценок (D4). Заморозка итоговых в term_grades (D14) появится
     * вместе с модулем grades: до него замораживать нечего, а веса СОР и СОЧ
     * берутся из нормативного акта, а не из кода (см. domain-rules.md, D12).
     */
    @Transactional
    public TermResponse close(Short id) {
        Term term = terms.findById(id).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        term.close();
        return TermResponse.from(terms.save(term));
    }

    /** Переоткрытие — обратимое решение завуча, поэтому идемпотентно. */
    @Transactional
    public TermResponse reopen(Short id) {
        Term term = terms.findById(id).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        term.reopen();
        return TermResponse.from(terms.save(term));
    }
}
