package com.princekumar.itams.asset;

import com.princekumar.itams.common.entity.BaseEntity;
import jakarta.persistence.*;

/**
 * A commercial asset model — e.g. "Lenovo ThinkPad T14 Gen 4". Many
 * {@link Asset} rows may share one model.
 *
 * <p>The {@code specs} column is JSONB in the database. It is exposed as a
 * plain {@code String} on the entity, so clients send and receive a JSON string.
 * A typed mapping would be the next step if specs need to be queried.</p>
 */
@Entity
@Table(name = "asset_model")
public class AssetModel extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private AssetCategory category;

    @Column(nullable = false, length = 80)
    private String manufacturer;

    @Column(name = "model_name", nullable = false, length = 160)
    private String modelName;

    @Column(columnDefinition = "jsonb")
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    private String specs;

    protected AssetModel() { /* JPA */ }

    public AssetModel(AssetCategory category, String manufacturer, String modelName, String specs) {
        this.category = category;
        this.manufacturer = manufacturer;
        this.modelName = modelName;
        this.specs = specs;
    }

    public AssetCategory getCategory() { return category; }
    public String getManufacturer() { return manufacturer; }
    public String getModelName() { return modelName; }
    public String getSpecs() { return specs; }
}
