package kz.bilimedu.api.school.dto;

import kz.bilimedu.api.school.Subject;

public record SubjectResponse(String id, String name) {

    public static SubjectResponse from(Subject subject) {
        return new SubjectResponse(String.valueOf(subject.getId()), subject.getName());
    }
}
