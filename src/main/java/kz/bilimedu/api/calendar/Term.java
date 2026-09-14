package kz.bilimedu.api.calendar;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Четверть с датами и признаком закрытия.
 *
 * <p>Пересечение четвертей внутри года запрещено ограничением
 * {@code EXCLUDE USING gist} — проверкой в коде это гарантировать нельзя,
 * два параллельных запроса прошли бы её оба.
 */
@Entity
@Table(name = "terms")
public class Term {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Short id;

    @Column(nullable = false)
    private Short academicYearId;

    @Column(nullable = false)
    private Short ordinal;

    @Column(nullable = false)
    private LocalDate startsOn;

    @Column(nullable = false)
    private LocalDate endsOn;

    /**
     * Закрытая четверть не принимает изменения оценок (правило D4). Правило
     * намеренно в сервисе, а не в базе: закрытие должно быть обратимым
     * решением завуча, а не жёстким запретом.
     */
    private Instant closedAt;

    protected Term() {
    }

    public Term(Short academicYearId, Short ordinal, LocalDate startsOn, LocalDate endsOn) {
        this.academicYearId = academicYearId;
        this.ordinal = ordinal;
        this.startsOn = startsOn;
        this.endsOn = endsOn;
    }

    public boolean isClosed() {
        return closedAt != null;
    }

    public void close() {
        if (closedAt == null) {
            this.closedAt = Instant.now();
        }
    }

    public void reopen() {
        this.closedAt = null;
    }

    public Short getId() {
        return id;
    }

    public Short getAcademicYearId() {
        return academicYearId;
    }

    public Short getOrdinal() {
        return ordinal;
    }

    public LocalDate getStartsOn() {
        return startsOn;
    }

    public LocalDate getEndsOn() {
        return endsOn;
    }

    public Instant getClosedAt() {
        return closedAt;
    }
}
