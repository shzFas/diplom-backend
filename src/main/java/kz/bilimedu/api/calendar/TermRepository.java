package kz.bilimedu.api.calendar;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TermRepository extends JpaRepository<Term, Short> {

    Page<Term> findByAcademicYearIdOrderByOrdinalAsc(Short academicYearId, Pageable pageable);

    Page<Term> findAllByOrderByAcademicYearIdAscOrdinalAsc(Pageable pageable);
}
