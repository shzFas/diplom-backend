package kz.bilimedu.api.school.dto;

import kz.bilimedu.api.school.TeachingAssignment;

public record AssignmentResponse(String id, String classId, String subjectId, String teacherId) {

    public static AssignmentResponse from(TeachingAssignment assignment) {
        return new AssignmentResponse(
                String.valueOf(assignment.getId()),
                String.valueOf(assignment.getClassId()),
                String.valueOf(assignment.getSubjectId()),
                String.valueOf(assignment.getTeacherId()));
    }
}
