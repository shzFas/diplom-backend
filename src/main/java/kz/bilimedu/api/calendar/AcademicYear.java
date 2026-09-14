package kz.bilimedu.api.calendar;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

/**
 * Учебный год. В версии 2023 года его не было вовсе, поэтому нельзя было
 * ни перейти на следующий год, ни отчитаться за прошлый: четверть была
 * строковым полем ktpPeriod, продублированным в каждом уроке и оценке.
 *
 * <p>Идентификатор — smallint (smallserial в схеме): учебных лет у школы
 * десятки, а не миллиарды.
 */
@Entity
@Table(name = "academic_years")
public class AcademicYear {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Short id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private LocalDate startsOn;

    @Column(nullable = false)
    private LocalDate endsOn;

    protected AcademicYear() {
    }

    public AcademicYear(String name, LocalDate startsOn, LocalDate endsOn) {
        this.name = name;
        this.startsOn = startsOn;
        this.endsOn = endsOn;
    }

    public Short getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public LocalDate getStartsOn() {
        return startsOn;
    }

    public LocalDate getEndsOn() {
        return endsOn;
    }
}
