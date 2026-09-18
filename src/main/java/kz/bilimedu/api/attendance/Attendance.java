package kz.bilimedu.api.attendance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Отметка посещаемости — отдельный домен от оценок (правило D10).
 *
 * <p>Возможны все четыре сочетания: ученик может отсутствовать и не иметь
 * оценки, отсутствовать и получить балл за отработку, присутствовать без
 * оценки и присутствовать с оценкой. Булев markFalse версии 2023 года
 * не выражал ни одного из этих случаев.
 */
@Entity
@Table(name = "attendance")
@IdClass(AttendanceId.class)
public class Attendance {

    @Id
    @Column(nullable = false)
    private Long lessonId;

    @Id
    @Column(nullable = false)
    private Long studentId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private AttendanceStatus status;

    @Column(nullable = false)
    private Long notedBy;

    // Как и в grades: триггера на updated_at в схеме нет.
    @Column(nullable = false, insertable = false)
    private Instant updatedAt;

    protected Attendance() {
    }

    public Attendance(Long lessonId, Long studentId, AttendanceStatus status, Long notedBy) {
        this.lessonId = lessonId;
        this.studentId = studentId;
        this.status = status;
        this.notedBy = notedBy;
    }

    @PreUpdate
    void touchUpdatedAt() {
        this.updatedAt = Instant.now();
    }

    public void mark(AttendanceStatus status, Long actorId) {
        this.status = status;
        this.notedBy = actorId;
    }

    public Long getLessonId() {
        return lessonId;
    }

    public Long getStudentId() {
        return studentId;
    }

    public AttendanceStatus getStatus() {
        return status;
    }

    public Long getNotedBy() {
        return notedBy;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
