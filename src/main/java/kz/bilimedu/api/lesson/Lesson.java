package kz.bilimedu.api.lesson;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Урок — бывший Ktp. В версии 2023 года он дублировал предмет, класс,
 * учителя и четверть строками внутри себя, а оценка дублировала их ещё раз;
 * источника истины не было, и правка урока не обновляла оценки.
 *
 * <p>Здесь всё это выводится через назначение: класс, предмет и учитель
 * берутся из teaching_assignments по assignment_id.
 */
@Entity
@Table(name = "lessons")
public class Lesson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long assignmentId;

    @Column(nullable = false)
    private Short termId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private LocalDate lessonDate;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private AssessmentKind kind;

    @Column(nullable = false)
    private Short maxScore;

    @Column(nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    protected Lesson() {
    }

    public Lesson(Long assignmentId, Short termId, String title, LocalDate lessonDate,
                  AssessmentKind kind, Short maxScore) {
        this.assignmentId = assignmentId;
        this.termId = termId;
        this.title = title;
        this.lessonDate = lessonDate;
        this.kind = kind;
        this.maxScore = maxScore;
    }

    public void rename(String title) {
        this.title = title;
    }

    public void moveTo(LocalDate lessonDate) {
        this.lessonDate = lessonDate;
    }

    public void changeMaxScore(Short maxScore) {
        this.maxScore = maxScore;
    }

    public Long getId() {
        return id;
    }

    public Long getAssignmentId() {
        return assignmentId;
    }

    public Short getTermId() {
        return termId;
    }

    public String getTitle() {
        return title;
    }

    public LocalDate getLessonDate() {
        return lessonDate;
    }

    public AssessmentKind getKind() {
        return kind;
    }

    public Short getMaxScore() {
        return maxScore;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
