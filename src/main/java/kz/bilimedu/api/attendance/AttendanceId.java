package kz.bilimedu.api.attendance;

import java.io.Serializable;
import java.util.Objects;

/**
 * Составной ключ таблицы attendance: одна отметка на ученика за урок.
 *
 * <p>Обычный класс, а не record: JPA требует от класса идентификатора
 * конструктор без аргументов.
 */
public class AttendanceId implements Serializable {

    private Long lessonId;
    private Long studentId;

    protected AttendanceId() {
    }

    public AttendanceId(Long lessonId, Long studentId) {
        this.lessonId = lessonId;
        this.studentId = studentId;
    }

    public Long getLessonId() {
        return lessonId;
    }

    public Long getStudentId() {
        return studentId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof AttendanceId that
                && Objects.equals(lessonId, that.lessonId)
                && Objects.equals(studentId, that.studentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lessonId, studentId);
    }
}
