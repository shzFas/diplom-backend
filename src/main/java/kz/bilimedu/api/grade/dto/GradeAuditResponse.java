package kz.bilimedu.api.grade.dto;

import java.time.Instant;
import kz.bilimedu.api.grade.GradeAction;
import kz.bilimedu.api.grade.GradeAudit;

public record GradeAuditResponse(String id, String gradeId, String studentId, String actorId,
                                 GradeAction action, Short oldScore, Short newScore,
                                 String reason, Instant at) {

    public static GradeAuditResponse from(GradeAudit audit) {
        return new GradeAuditResponse(
                String.valueOf(audit.getId()),
                String.valueOf(audit.getGradeId()),
                String.valueOf(audit.getStudentId()),
                String.valueOf(audit.getActorId()),
                audit.getAction(),
                audit.getOldScore(),
                audit.getNewScore(),
                audit.getReason(),
                audit.getAt());
    }
}
