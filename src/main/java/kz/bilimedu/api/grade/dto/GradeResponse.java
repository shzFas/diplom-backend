package kz.bilimedu.api.grade.dto;

import kz.bilimedu.api.grade.Grade;

public record GradeResponse(String id, String lessonId, String studentId, short score, String gradedBy) {

    public static GradeResponse from(Grade grade) {
        return new GradeResponse(
                String.valueOf(grade.getId()),
                String.valueOf(grade.getLessonId()),
                String.valueOf(grade.getStudentId()),
                grade.getScore(),
                String.valueOf(grade.getGradedBy()));
    }
}
