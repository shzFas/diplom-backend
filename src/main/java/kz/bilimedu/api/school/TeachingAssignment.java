package kz.bilimedu.api.school;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * «Кто какой предмет ведёт в каком классе» — центральная сущность модели:
 * на неё вешаются уроки, через неё считаются итоговые, ею же проверяются
 * права учителя (предикат P1 из permissions.md).
 *
 * <p>Заменяет сразу два нетипизированных массива версии 2023 года:
 * Predmet.classes[] и User.permission[].
 */
@Entity
@Table(name = "teaching_assignments")
public class TeachingAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long classId;

    @Column(nullable = false)
    private Long subjectId;

    @Column(nullable = false)
    private Long teacherId;

    protected TeachingAssignment() {
    }

    public TeachingAssignment(Long classId, Long subjectId, Long teacherId) {
        this.classId = classId;
        this.subjectId = subjectId;
        this.teacherId = teacherId;
    }

    public Long getId() {
        return id;
    }

    public Long getClassId() {
        return classId;
    }

    public Long getSubjectId() {
        return subjectId;
    }

    public Long getTeacherId() {
        return teacherId;
    }
}
