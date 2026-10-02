package com.princekumar.itams.maintenance;

import com.princekumar.itams.asset.Asset;
import com.princekumar.itams.common.entity.BaseEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A single maintenance / repair event against an asset. Either performed
 * internally (by a user account) or externally (by a named provider) —
 * at least one must be set (CHECK constraint at DB level).
 */
@Entity
@Table(name = "maintenance_record")
public class MaintenanceRecord extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false, updatable = false)
    private Asset asset;

    @Column(name = "performed_on", nullable = false)
    private LocalDate performedOn;

    @Column(name = "performed_by_user_id")
    private Long performedByUserId;

    @Column(name = "provider_name", length = 160)
    private String providerName;

    @Column(columnDefinition = "text", nullable = false)
    private String description;

    @Column(precision = 12, scale = 2)
    private BigDecimal cost;

    @Column(name = "next_scheduled_on")
    private LocalDate nextScheduledOn;

    protected MaintenanceRecord() { /* JPA */ }

    public MaintenanceRecord(Asset asset, LocalDate performedOn, Long performedByUserId,
                             String providerName, String description, BigDecimal cost,
                             LocalDate nextScheduledOn) {
        this.asset = asset;
        this.performedOn = performedOn;
        this.performedByUserId = performedByUserId;
        this.providerName = providerName;
        this.description = description;
        this.cost = cost;
        this.nextScheduledOn = nextScheduledOn;
    }

    public Asset getAsset() { return asset; }
    public LocalDate getPerformedOn() { return performedOn; }
    public Long getPerformedByUserId() { return performedByUserId; }
    public String getProviderName() { return providerName; }
    public String getDescription() { return description; }
    public BigDecimal getCost() { return cost; }
    public LocalDate getNextScheduledOn() { return nextScheduledOn; }
}
