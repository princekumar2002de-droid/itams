package com.princekumar.itams.license;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SoftwareLicenseRepository extends JpaRepository<SoftwareLicense, Long> {

    /**
     * Loads the associations the API response reads ("product") in the same query.
     * open-in-view is off, so anything not loaded here would throw
     * LazyInitializationException when the controller maps the entity.
     */
    @EntityGraph(attributePaths = {"product"})
    @Override
    Optional<SoftwareLicense> findById(Long id);

    /** SELECT ... FOR UPDATE used by seat-allocation to serialise concurrent assign attempts. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM SoftwareLicense l WHERE l.id = :id")
    Optional<SoftwareLicense> findByIdForUpdate(@Param("id") Long id);

    @Query("""
        SELECT l FROM SoftwareLicense l
         WHERE (:q IS NULL OR :q = ''
                OR LOWER(l.product.name) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(l.product.vendor) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(l.licenseReference) LIKE LOWER(CONCAT('%', :q, '%')))
    """)
    @EntityGraph(attributePaths = {"product"})
    Page<SoftwareLicense> search(@Param("q") String q, Pageable pageable);
}
