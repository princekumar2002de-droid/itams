package com.princekumar.itams.asset;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AssetRepository extends JpaRepository<Asset, Long> {

    /**
     * Loads the associations the API response reads ("model", "model.category") in the same query.
     * open-in-view is off, so anything not loaded here would throw
     * LazyInitializationException when the controller maps the entity.
     */
    @EntityGraph(attributePaths = {"model", "model.category"})
    @Override
    Optional<Asset> findById(Long id);

    boolean existsByAssetTag(String assetTag);

    /**
     * SELECT ... FOR UPDATE — used by {@code AssetAssignmentService} to
     * serialise concurrent assignment attempts on the same asset.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Asset a WHERE a.id = :id")
    Optional<Asset> findByIdForUpdate(@Param("id") Long id);

    @Query("""
        SELECT a FROM Asset a
         WHERE (:status IS NULL OR a.status = :status)
           AND (:categoryId IS NULL OR a.model.category.id = :categoryId)
           AND (:q IS NULL OR :q = ''
                OR LOWER(a.assetTag)     LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(a.serialNumber) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(a.model.manufacturer) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(a.model.modelName)    LIKE LOWER(CONCAT('%', :q, '%')))
    """)
    @EntityGraph(attributePaths = {"model", "model.category"})
    Page<Asset> search(String q, AssetStatus status, Long categoryId, Pageable pageable);
}
