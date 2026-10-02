package com.princekumar.itams.department;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<Department, Long> {

    /**
     * A "live" row is one that hasn't been soft-deleted. Almost every
     * read path uses this filter — soft-deleted rows are only for admin
     * queries and the audit trail.
     */
    Optional<Department> findByIdAndDeletedAtIsNull(Long id);

    /** Case-sensitive; DB uniqueness index is case-insensitive on {@code code}. */
    boolean existsByCodeIgnoreCaseAndDeletedAtIsNull(String code);

    @Query("""
        SELECT d FROM Department d
         WHERE d.deletedAt IS NULL
           AND (:q IS NULL OR :q = ''
                OR LOWER(d.code) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(d.name) LIKE LOWER(CONCAT('%', :q, '%')))
    """)
    Page<Department> searchLive(String q, Pageable pageable);
}
