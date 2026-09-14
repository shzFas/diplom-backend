package kz.bilimedu.api.school;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Класс-группа существует внутри учебного года: «8А» 2025/26 и «9А» 2026/27 —
 * разные строки. Переход на следующий год не мутирует прошлое, поэтому
 * прошлогодние оценки остаются оценками прошлогоднего класса.
 */
@Entity
@Table(name = "classes")
public class SchoolClass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Short academicYearId;

    @Column(nullable = false)
    private String name;

    protected SchoolClass() {
    }

    public SchoolClass(Short academicYearId, String name) {
        this.academicYearId = academicYearId;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public Short getAcademicYearId() {
        return academicYearId;
    }

    public String getName() {
        return name;
    }
}
