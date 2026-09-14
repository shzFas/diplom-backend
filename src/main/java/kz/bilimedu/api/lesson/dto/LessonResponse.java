package kz.bilimedu.api.lesson.dto;

import java.time.LocalDate;
import kz.bilimedu.api.lesson.AssessmentKind;
import kz.bilimedu.api.lesson.Lesson;

public record LessonResponse(String id, String assignmentId, String termId, String title,
                             LocalDate lessonDate, AssessmentKind kind, short maxScore) {

    public static LessonResponse from(Lesson lesson) {
        return new LessonResponse(
                String.valueOf(lesson.getId()),
                String.valueOf(lesson.getAssignmentId()),
                String.valueOf(lesson.getTermId()),
                lesson.getTitle(),
                lesson.getLessonDate(),
                lesson.getKind(),
                lesson.getMaxScore());
    }
}
