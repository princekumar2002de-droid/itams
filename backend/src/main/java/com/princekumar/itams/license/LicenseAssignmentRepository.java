package com.princekumar.itams.license;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;

public interface LicenseAssignmentRepository extends JpaRepository<LicenseAssignment, Long> {

    /**
     * Loads the associations the API response reads ("person", "asset") in the same query.
     * open-in-view is off, so anything not loaded here would throw
     * LazyInitializationException when the controller maps the entity.
     */
    @EntityGraph(attributePaths = {"person", "asset"})
    @Override
    Optional<LicenseAssignment> findById(Long id);

    @Query("SELECT COUNT(la) FROM LicenseAssignment la WHERE la.license.id = :licenseId AND la.releasedAt IS NULL")
    long countOpenByLicenseId(@Param("licenseId") Long licenseId);

    @Query("SELECT la FROM LicenseAssignment la WHERE la.license.id = :licenseId ORDER BY la.assignedAt DESC")
    @EntityGraph(attributePaths = {"person", "asset"})
    List<LicenseAssignment> findByLicenseId(@Param("licenseId") Long licenseId);
}
