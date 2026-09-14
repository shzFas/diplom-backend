package kz.bilimedu.api.school.dto;

import kz.bilimedu.api.school.SchoolClass;

public record ClassResponse(String id, String academicYearId, String name) {

    public static ClassResponse from(SchoolClass schoolClass) {
        return new ClassResponse(
                String.valueOf(schoolClass.getId()),
                String.valueOf(schoolClass.getAcademicYearId()),
                schoolClass.getName());
    }
}
