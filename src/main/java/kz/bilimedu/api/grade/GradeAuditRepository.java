package kz.bilimedu.api.grade;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GradeAuditRepository extends JpaRepository<GradeAudit, Long> {

    /** История правок одной оценки — от свежих к старым. */
    Page<GradeAudit> findByGradeIdOrderByAtDescIdDesc(Long gradeId, Pageable pageable);
}
