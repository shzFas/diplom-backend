package kz.bilimedu.api.school;

import kz.bilimedu.api.calendar.AcademicYearRepository;
import kz.bilimedu.api.common.ApiException;
import kz.bilimedu.api.common.ErrorCode;
import kz.bilimedu.api.common.PageResponse;
import kz.bilimedu.api.school.dto.AssignmentResponse;
import kz.bilimedu.api.school.dto.ClassResponse;
import kz.bilimedu.api.school.dto.CreateAssignmentRequest;
import kz.bilimedu.api.school.dto.CreateClassRequest;
import kz.bilimedu.api.school.dto.CreateSubjectRequest;
import kz.bilimedu.api.school.dto.SubjectResponse;
import kz.bilimedu.api.security.AuthenticatedUser;
import kz.bilimedu.api.user.Role;
import kz.bilimedu.api.user.User;
import kz.bilimedu.api.user.UserRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Классы, предметы и назначения учителей. */
@Service
public class SchoolService {

    private final SchoolClassRepository classes;
    private final SubjectRepository subjects;
    private final TeachingAssignmentRepository assignments;
    private final AcademicYearRepository years;
    private final UserRepository users;

    public SchoolService(SchoolClassRepository classes,
                         SubjectRepository subjects,
                         TeachingAssignmentRepository assignments,
                         AcademicYearRepository years,
                         UserRepository users) {
        this.classes = classes;
        this.subjects = subjects;
        this.assignments = assignments;
        this.years = years;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public PageResponse<ClassResponse> listClasses(AuthenticatedUser viewer, Short academicYearId,
                                                   Pageable pageable) {
        return PageResponse.of(classes
                .findVisible(viewer.id(), viewer.role().name(), academicYearId, pageable)
                .map(ClassResponse::from));
    }

    @Transactional
    public ClassResponse createClass(CreateClassRequest request) {
        if (!years.existsById(request.academicYearId())) {
            throw new ApiException(ErrorCode.NOT_FOUND);
        }
        SchoolClass created = classes.save(
                new SchoolClass(request.academicYearId(), request.name().strip()));
        return ClassResponse.from(created);
    }

    @Transactional(readOnly = true)
    public PageResponse<SubjectResponse> listSubjects(AuthenticatedUser viewer, Long classId,
                                                      Pageable pageable) {
        return PageResponse.of(subjects
                .findVisible(viewer.id(), viewer.role().name(), classId, pageable)
                .map(SubjectResponse::from));
    }

    @Transactional
    public SubjectResponse createSubject(CreateSubjectRequest request) {
        return SubjectResponse.from(subjects.save(new Subject(request.name().strip())));
    }

    @Transactional(readOnly = true)
    public PageResponse<AssignmentResponse> listAssignments(AuthenticatedUser viewer, Long teacherId,
                                                            Long classId, Pageable pageable) {
        return PageResponse.of(assignments
                .findVisible(viewer.id(), viewer.role().name(), teacherId, classId, pageable)
                .map(AssignmentResponse::from));
    }

    @Transactional
    public AssignmentResponse createAssignment(CreateAssignmentRequest request) {
        if (!classes.existsById(request.classId()) || !subjects.existsById(request.subjectId())) {
            throw new ApiException(ErrorCode.NOT_FOUND);
        }
        User teacher = users.findById(request.teacherId())
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));

        // Роль проверяется здесь, а не ограничением базы: FK ведёт на users,
        // где лежат все три роли, и «ученик ведёт математику» прошёл бы.
        if (teacher.getRole() != Role.TEACHER || !teacher.isActive()) {
            throw new ApiException(ErrorCode.NOT_A_TEACHER);
        }

        TeachingAssignment created = assignments.save(new TeachingAssignment(
                request.classId(), request.subjectId(), request.teacherId()));
        return AssignmentResponse.from(created);
    }

    /**
     * Удаление назначения. Уроки ссылаются на него с ON DELETE CASCADE,
     * поэтому удаление назначения с уроками молча унесло бы журнал вместе
     * с оценками — такие назначения удалять запрещено.
     */
    @Transactional
    public void deleteAssignment(Long id) {
        if (!assignments.existsById(id)) {
            throw new ApiException(ErrorCode.NOT_FOUND);
        }
        if (assignments.hasLessons(id)) {
            throw new ApiException(ErrorCode.ASSIGNMENT_HAS_LESSONS);
        }
        assignments.deleteById(id);
    }
}
