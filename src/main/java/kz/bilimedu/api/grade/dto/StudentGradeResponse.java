package kz.bilimedu.api.grade.dto;

import java.time.LocalDate;
import kz.bilimedu.api.grade.StudentGradeRow;
import kz.bilimedu.api.lesson.AssessmentKind;

public record StudentGradeResponse(String gradeId, String lessonId, String lessonTitle,
                                   LocalDate lessonDate, AssessmentKind kind,
                                   short score, short maxScore,
                                   String subjectId, String subjectName, String termId) {

    public static StudentGradeResponse from(StudentGradeRow row) {
        return new StudentGradeResponse(
                String.valueOf(row.gradeId()),
                String.valueOf(row.lessonId()),
                row.lessonTitle(),
                row.lessonDate(),
                row.kind(),
                row.score(),
                row.maxScore(),
                String.valueOf(row.subjectId()),
                row.subjectName(),
                String.valueOf(row.termId()));
    }
}
