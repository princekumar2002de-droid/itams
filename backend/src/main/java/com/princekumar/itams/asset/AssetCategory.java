package com.princekumar.itams.asset;

import com.princekumar.itams.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Broad classification of an asset (LAPTOP, MONITOR, PHONE, ...). Seeded
 * in V2 and treated as read-only reference data by the API; there is no
 * endpoint to change categories.
 */
@Entity
@Table(name = "asset_category")
public class AssetCategory extends BaseEntity {

    @Column(nullable = false, length = 30, unique = true, updatable = false)
    private String code;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 255)
    private String description;

    protected AssetCategory() { /* JPA */ }

    public String getCode() { return code; }
    public String getName() { return name; }
    public String getDescription() { return description; }
}
