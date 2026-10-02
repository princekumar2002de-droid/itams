package com.princekumar.itams.assignment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface AssetAssignmentRepository extends JpaRepository<AssetAssignment, Long> {

    /**
     * Loads the associations the API response reads ("asset", "assignee") in the same query.
     * open-in-view is off, so anything not loaded here would throw
     * LazyInitializationException when the controller maps the entity.
     */
    @EntityGraph(attributePaths = {"asset", "assignee"})
    @Override
    Optional<AssetAssignment> findById(Long id);

    /** The single OPEN assignment for an asset, if any. */
    @Query("""
        SELECT a FROM AssetAssignment a
         WHERE a.asset.id = :assetId
           AND a.actualReturnAt IS NULL
    """)
    @EntityGraph(attributePaths = {"asset", "assignee"})
    Optional<AssetAssignment> findOpenByAssetId(Long assetId);

    @Query("""
        SELECT a FROM AssetAssignment a
         WHERE (:assetId IS NULL OR a.asset.id = :assetId)
           AND (:personId IS NULL OR a.assignee.id = :personId)
           AND (:onlyOpen = FALSE OR a.actualReturnAt IS NULL)
    """)
    @EntityGraph(attributePaths = {"asset", "assignee"})
    Page<AssetAssignment> search(Long assetId, Long personId, boolean onlyOpen, Pageable pageable);
}
