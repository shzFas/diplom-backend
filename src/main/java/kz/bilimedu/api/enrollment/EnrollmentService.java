package kz.bilimedu.api.enrollment;

import java.time.LocalDate;
import kz.bilimedu.api.common.ApiException;
import kz.bilimedu.api.common.ErrorCode;
import kz.bilimedu.api.common.PageResponse;
import kz.bilimedu.api.enrollment.dto.CreateEnrollmentRequest;
import kz.bilimedu.api.enrollment.dto.EnrollmentResponse;
import kz.bilimedu.api.enrollment.dto.RosterEntryResponse;
import kz.bilimedu.api.enrollment.dto.TransferRequest;
import kz.bilimedu.api.school.SchoolClassRepository;
import kz.bilimedu.api.security.AuthenticatedUser;
import kz.bilimedu.api.user.Role;
import kz.bilimedu.api.user.User;
import kz.bilimedu.api.user.UserRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Зачисления и переводы.
 *
 * <p>Перевод не меняет класс у ученика, а закрывает одно зачисление и
 * открывает другое. Благодаря этому состав класса на любую прошлую дату
 * получается запросом, а прошлогодние оценки остаются привязанными
 * к прошлогоднему классу.
 */
@Service
public class EnrollmentService {

    private final EnrollmentRepository enrollments;
    private final SchoolClassRepository classes;
    private final UserRepository users;

    public EnrollmentService(EnrollmentRepository enrollments,
                             SchoolClassRepository classes,
                             UserRepository users) {
        this.enrollments = enrollments;
        this.classes = classes;
        this.users = users;
    }

    @Transactional
    public EnrollmentResponse enroll(CreateEnrollmentRequest request) {
        User student = users.findById(request.studentId())
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));

        // Роль не выражается внешним ключом: он ведёт на users, где лежат
        // все три роли, и «учитель зачислен в 8А» прошёл бы беспрепятственно.
        if (student.getRole() != Role.STUDENT || !student.isActive()) {
            throw new ApiException(ErrorCode.NOT_A_STUDENT);
        }
        if (!classes.existsById(request.classId())) {
            throw new ApiException(ErrorCode.NOT_FOUND);
        }

        // Пересечение с уже существующим зачислением ловит EXCLUDE USING gist
        // и переводит в ENROLLMENT_OVERLAPS: проверкой в коде это не
        // гарантируется, два параллельных запроса прошли бы её оба.
        Enrollment saved = enrollments.save(
                new Enrollment(request.studentId(), request.classId(), request.fromDate()));
        return response(saved.getId());
    }

    @Transactional
    public EnrollmentResponse transfer(Long enrollmentId, TransferRequest request) {
        Enrollment current = enrollments.findById(enrollmentId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));

        if (!current.isActive()) {
            throw new ApiException(ErrorCode.ENROLLMENT_NOT_ACTIVE);
        }
        if (current.getClassId().equals(request.toClassId())) {
            throw new ApiException(ErrorCode.ALREADY_IN_CLASS);
        }
        if (!classes.existsById(request.toClassId())) {
            throw new ApiException(ErrorCode.NOT_FOUND);
        }

        // Прежнее зачисление закрывается днём до перехода. Если перевод
        // датирован днём начала прежнего зачисления или раньше, закрывающая
        // дата оказалась бы меньше открывающей — это нарушение CHECK
        // enrollments_check, но клиенту полезнее внятная ошибка валидации.
        LocalDate lastDayInOldClass = request.fromDate().minusDays(1);
        if (lastDayInOldClass.isBefore(current.getFromDate())) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED);
        }

        current.closeOn(lastDayInOldClass);

        // Порядок операций важен: Hibernate в одном flush выполняет вставки
        // раньше обновлений, поэтому без принудительного flush новая запись
        // ушла бы в базу до закрытия прежней и упёрлась в EXCLUDE.
        enrollments.saveAndFlush(current);

        Enrollment moved = enrollments.save(
                new Enrollment(current.getStudentId(), request.toClassId(), request.fromDate()));
        return response(moved.getId());
    }

    /**
     * Состав класса на дату. Ученику не показывается вовсе: по матрице прав
     * он читает только свои зачисления, а не список одноклассников.
     */
    @Transactional(readOnly = true)
    public PageResponse<RosterEntryResponse> roster(AuthenticatedUser viewer, Long classId,
                                                    LocalDate on, Pageable pageable) {
        if (!classes.existsById(classId)) {
            throw new ApiException(ErrorCode.NOT_FOUND);
        }
        if (viewer.role() == Role.TEACHER && !enrollments.teacherTeachesClass(viewer.id(), classId)) {
            throw new ApiException(ErrorCode.NOT_OWNER);
        }
        LocalDate date = on == null ? LocalDate.now() : on;
        return PageResponse.of(enrollments.roster(classId, date, pageable).map(RosterEntryResponse::from));
    }

    @Transactional(readOnly = true)
    public PageResponse<EnrollmentResponse> history(AuthenticatedUser viewer, Long studentId,
                                                    Pageable pageable) {
        if (!canReadStudent(viewer, studentId)) {
            throw new ApiException(ErrorCode.NOT_OWNER);
        }
        return PageResponse.of(enrollments.history(studentId, pageable).map(EnrollmentResponse::from));
    }

    private boolean canReadStudent(AuthenticatedUser viewer, Long studentId) {
        return switch (viewer.role()) {
            case ADMIN -> true;
            case STUDENT -> viewer.id().equals(studentId);
            case TEACHER -> users.teacherTeachesStudent(viewer.id(), studentId);
        };
    }

    private EnrollmentResponse response(Long enrollmentId) {
        return enrollments.row(enrollmentId)
                .map(EnrollmentResponse::from)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
    }
}
