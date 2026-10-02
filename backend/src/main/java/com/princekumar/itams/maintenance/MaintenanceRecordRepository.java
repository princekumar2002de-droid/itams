package com.princekumar.itams.maintenance;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;

public interface MaintenanceRecordRepository extends JpaRepository<MaintenanceRecord, Long> {

    /**
     * Loads the associations the API response reads ("asset") in the same query.
     * open-in-view is off, so anything not loaded here would throw
     * LazyInitializationException when the controller maps the entity.
     */
    @EntityGraph(attributePaths = {"asset"})
    @Override
    Optional<MaintenanceRecord> findById(Long id);

    @Query("SELECT m FROM MaintenanceRecord m WHERE m.asset.id = :assetId ORDER BY m.performedOn DESC")
    @EntityGraph(attributePaths = {"asset"})
    List<MaintenanceRecord> findByAssetId(@Param("assetId") Long assetId);

    @Query("""
        SELECT m FROM MaintenanceRecord m
         WHERE (:assetId IS NULL OR m.asset.id = :assetId)
           AND (:q IS NULL OR :q = ''
                OR LOWER(m.description) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(m.providerName) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(m.asset.assetTag) LIKE LOWER(CONCAT('%', :q, '%')))
        ORDER BY m.performedOn DESC
    """)
    @EntityGraph(attributePaths = {"asset"})
    Page<MaintenanceRecord> search(@Param("q") String q, @Param("assetId") Long assetId, Pageable pageable);
}
