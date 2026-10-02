package com.princekumar.itams.asset;

import com.princekumar.itams.common.entity.BaseEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * A single physical asset (one specific laptop, one specific monitor).
 *
 * <p>Immutable at the API surface: {@code assetTag} and {@code serialNumber}
 * cannot be updated once set — the service layer enforces this. Status
 * transitions go through {@link AssetService}, not by callers setting the
 * field directly.</p>
 */
@Entity
@Table(name = "asset")
public class Asset extends BaseEntity {

    @Column(name = "asset_tag", nullable = false, length = 30, unique = true, updatable = false)
    private String assetTag;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "model_id", nullable = false, updatable = false)
    private AssetModel model;

    @Column(name = "serial_number", nullable = false, length = 80, updatable = false)
    private String serialNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AssetStatus status = AssetStatus.IN_STOCK;

    @Column(name = "purchase_date", nullable = false)
    private LocalDate purchaseDate;

    @Column(name = "purchase_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal purchasePrice;

    @Column(name = "warranty_ends_on")
    private LocalDate warrantyEndsOn;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(name = "retired_at")
    private OffsetDateTime retiredAt;

    @Column(name = "retired_reason", length = 255)
    private String retiredReason;

    protected Asset() { /* JPA */ }

    public Asset(String assetTag, AssetModel model, String serialNumber,
                 LocalDate purchaseDate, BigDecimal purchasePrice,
                 LocalDate warrantyEndsOn, String notes) {
        this.assetTag = assetTag;
        this.model = model;
        this.serialNumber = serialNumber;
        this.purchaseDate = purchaseDate;
        this.purchasePrice = purchasePrice;
        this.warrantyEndsOn = warrantyEndsOn;
        this.notes = notes;
    }

    // ── behaviour (called only by AssetService) ──────────────────────────────

    public void markAssigned() {
        if (status != AssetStatus.IN_STOCK) {
            throw new IllegalStateException("Cannot assign asset with status " + status);
        }
        this.status = AssetStatus.ASSIGNED;
    }

    public void markReturned(boolean sendForMaintenance) {
        if (status != AssetStatus.ASSIGNED) {
            throw new IllegalStateException("Cannot return asset with status " + status);
        }
        this.status = sendForMaintenance ? AssetStatus.UNDER_MAINTENANCE : AssetStatus.IN_STOCK;
    }

    public void markMaintenanceDone() {
        if (status != AssetStatus.UNDER_MAINTENANCE) {
            throw new IllegalStateException("Cannot complete maintenance for status " + status);
        }
        this.status = AssetStatus.IN_STOCK;
    }

    public void retire(String reason) {
        if (status == AssetStatus.RETIRED || status == AssetStatus.LOST || status == AssetStatus.ASSIGNED) {
            throw new IllegalStateException("Cannot retire asset with status " + status);
        }
        this.status = AssetStatus.RETIRED;
        this.retiredAt = OffsetDateTime.now();
        this.retiredReason = reason;
    }

    // ── getters / setters ────────────────────────────────────────────────────

    public String getAssetTag() { return assetTag; }
    public AssetModel getModel() { return model; }
    public String getSerialNumber() { return serialNumber; }
    public AssetStatus getStatus() { return status; }
    public LocalDate getPurchaseDate() { return purchaseDate; }
    public BigDecimal getPurchasePrice() { return purchasePrice; }
    public LocalDate getWarrantyEndsOn() { return warrantyEndsOn; }
    public void setWarrantyEndsOn(LocalDate v) { this.warrantyEndsOn = v; }
    public String getNotes() { return notes; }
    public void setNotes(String v) { this.notes = v; }
    public OffsetDateTime getRetiredAt() { return retiredAt; }
    public String getRetiredReason() { return retiredReason; }
}
