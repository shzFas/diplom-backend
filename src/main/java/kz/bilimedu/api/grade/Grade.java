package kz.bilimedu.api.grade;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Оценка. В версии 2023 года Mark дублировал семь полей из Ktp
 * (markPredmet, markClassStudent, markTeacher, markMaxValue, markSochSor,
 * markPeriod, markDate) без источника истины, и правка урока их не обновляла.
 *
 * <p>Здесь всё это выводится через урок: предмет, класс, четверть и максимум
 * балла берутся по lesson_id. Автор остаётся в graded_by и не теряется,
 * даже если учителя уволили — поэтому учитель деактивируется, а не удаляется.
 */
@Entity
@Table(name = "grades")
public class Grade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long lessonId;

    @Column(nullable = false)
    private Long studentId;

    @Column(nullable = false)
    private Short score;

    @Column(nullable = false)
    private Long gradedBy;

    @Column(nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    // Триггера на updated_at в схеме нет, иначе поле было бы декоративным.
    @Column(nullable = false, insertable = false)
    private Instant updatedAt;

    protected Grade() {
    }

    public Grade(Long lessonId, Long studentId, Short score, Long gradedBy) {
        this.lessonId = lessonId;
        this.studentId = studentId;
        this.score = score;
        this.gradedBy = gradedBy;
    }

    @PreUpdate
    void touchUpdatedAt() {
        this.updatedAt = Instant.now();
    }

    /** Переставить балл, зафиксировав нового автора правки. */
    public void rescore(Short score, Long actorId) {
        this.score = score;
        this.gradedBy = actorId;
    }

    public Long getId() {
        return id;
    }

    public Long getLessonId() {
        return lessonId;
    }

    public Long getStudentId() {
        return studentId;
    }

    public Short getScore() {
        return score;
    }

    public Long getGradedBy() {
        return gradedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
