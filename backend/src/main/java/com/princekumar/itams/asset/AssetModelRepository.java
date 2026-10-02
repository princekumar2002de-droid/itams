package com.princekumar.itams.asset;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;

public interface AssetModelRepository extends JpaRepository<AssetModel, Long> {

    /**
     * Loads the associations the API response reads ("category") in the same query.
     * open-in-view is off, so anything not loaded here would throw
     * LazyInitializationException when the controller maps the entity.
     */
    @EntityGraph(attributePaths = {"category"})
    @Override
    Optional<AssetModel> findById(Long id);

    boolean existsByManufacturerAndModelName(String manufacturer, String modelName);

    @Query("""
        SELECT m FROM AssetModel m
         WHERE (:categoryId IS NULL OR m.category.id = :categoryId)
           AND (:q IS NULL OR :q = ''
                OR LOWER(m.manufacturer) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(m.modelName)    LIKE LOWER(CONCAT('%', :q, '%')))
    """)
    @EntityGraph(attributePaths = {"category"})
    Page<AssetModel> search(String q, Long categoryId, Pageable pageable);
}
