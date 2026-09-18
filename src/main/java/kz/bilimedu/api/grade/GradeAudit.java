package kz.bilimedu.api.grade;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Запись аудита правки оценки — юридически значимое событие. В версии
 * 2023 года deleteOne удалял оценку бесследно.
 *
 * <p>Поле gradeId намеренно без внешнего ключа: запись должна переживать
 * удаление самой оценки (правило D17), иначе смысл аудита теряется ровно
 * в том случае, ради которого он и нужен.
 */
@Entity
@Table(name = "grade_audit")
public class GradeAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long gradeId;

    @Column(nullable = false)
    private Long lessonId;

    @Column(nullable = false)
    private Long studentId;

    @Column(nullable = false)
    private Long actorId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private GradeAction action;

    private Short oldScore;

    private Short newScore;

    private String reason;

    @Column(name = "at", nullable = false, insertable = false, updatable = false)
    private Instant at;

    protected GradeAudit() {
    }

    private GradeAudit(Long gradeId, Long lessonId, Long studentId, Long actorId,
                       GradeAction action, Short oldScore, Short newScore, String reason) {
        this.gradeId = gradeId;
        this.lessonId = lessonId;
        this.studentId = studentId;
        this.actorId = actorId;
        this.action = action;
        this.oldScore = oldScore;
        this.newScore = newScore;
        this.reason = reason;
    }

    public static GradeAudit created(Grade grade, Long actorId) {
        return new GradeAudit(grade.getId(), grade.getLessonId(), grade.getStudentId(), actorId,
                GradeAction.CREATED, null, grade.getScore(), null);
    }

    public static GradeAudit updated(Grade grade, Short oldScore, Long actorId, String reason) {
        return new GradeAudit(grade.getId(), grade.getLessonId(), grade.getStudentId(), actorId,
                GradeAction.UPDATED, oldScore, grade.getScore(), reason);
    }

    public static GradeAudit deleted(Grade grade, Long actorId, String reason) {
        return new GradeAudit(grade.getId(), grade.getLessonId(), grade.getStudentId(), actorId,
                GradeAction.DELETED, grade.getScore(), null, reason);
    }

    public Long getId() {
        return id;
    }

    public Long getGradeId() {
        return gradeId;
    }

    public Long getStudentId() {
        return studentId;
    }

    public Long getActorId() {
        return actorId;
    }

    public GradeAction getAction() {
        return action;
    }

    public Short getOldScore() {
        return oldScore;
    }

    public Short getNewScore() {
        return newScore;
    }

    public String getReason() {
        return reason;
    }

    public Instant getAt() {
        return at;
    }
}
