package kz.bilimedu.api.enrollment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

/**
 * Зачисление — связь ученика с классом, имеющая даты, а не атрибут ученика.
 *
 * <p>В версии 2023 года это было поле Student.classId, и перевод ученика
 * перезаписывал историю: прошлогодние оценки начинали выглядеть как оценки
 * нового класса. Здесь перевод — закрытие одной записи и открытие другой,
 * а прошлое остаётся неизменным.
 *
 * <p>Ограничение {@code EXCLUDE USING gist} не даёт ученику числиться
 * в двух классах одновременно; проверкой в коде это не гарантируется.
 */
@Entity
@Table(name = "enrollments")
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long studentId;

    @Column(nullable = false)
    private Long classId;

    @Column(nullable = false)
    private LocalDate fromDate;

    /** Последний день обучения в классе, включительно. NULL — зачисление активно. */
    private LocalDate toDate;

    protected Enrollment() {
    }

    public Enrollment(Long studentId, Long classId, LocalDate fromDate) {
        this.studentId = studentId;
        this.classId = classId;
        this.fromDate = fromDate;
    }

    public boolean isActive() {
        return toDate == null;
    }

    public void closeOn(LocalDate lastDay) {
        this.toDate = lastDay;
    }

    public Long getId() {
        return id;
    }

    public Long getStudentId() {
        return studentId;
    }

    public Long getClassId() {
        return classId;
    }

    public LocalDate getFromDate() {
        return fromDate;
    }

    public LocalDate getToDate() {
        return toDate;
    }
}
