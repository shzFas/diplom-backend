package kz.bilimedu.api.grade;

import java.time.LocalDate;
import kz.bilimedu.api.lesson.AssessmentKind;

/** Оценка ученика вместе с уроком и предметом — чтобы дневник читался одним запросом. */
public record StudentGradeRow(Long gradeId, Long lessonId, String lessonTitle, LocalDate lessonDate,
                              AssessmentKind kind, Short score, Short maxScore,
                              Long subjectId, String subjectName, Short termId) {
}
