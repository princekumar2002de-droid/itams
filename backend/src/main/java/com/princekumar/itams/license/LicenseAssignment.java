package com.princekumar.itams.license;

import com.princekumar.itams.asset.Asset;
import com.princekumar.itams.common.entity.BaseEntity;
import com.princekumar.itams.person.Person;
import jakarta.persistence.*;

import java.time.OffsetDateTime;

/**
 * A single seat of a license, allocated to EITHER a person OR an asset
 * (never both, never neither — enforced at the DB by a CHECK constraint).
 */
@Entity
@Table(name = "license_assignment")
public class LicenseAssignment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "license_id", nullable = false, updatable = false)
    private SoftwareLicense license;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id", updatable = false)
    private Person person;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id", updatable = false)
    private Asset asset;

    @Column(name = "assigned_at", nullable = false, updatable = false)
    private OffsetDateTime assignedAt;

    @Column(name = "released_at")
    private OffsetDateTime releasedAt;

    @Column(columnDefinition = "text")
    private String notes;

    protected LicenseAssignment() { /* JPA */ }

    public LicenseAssignment(SoftwareLicense license, Person person, Asset asset, String notes) {
        if ((person == null) == (asset == null)) {
            throw new IllegalArgumentException("Exactly one of person / asset must be non-null");
        }
        this.license = license;
        this.person = person;
        this.asset = asset;
        this.notes = notes;
        this.assignedAt = OffsetDateTime.now();
    }

    public void release() {
        if (this.releasedAt == null) this.releasedAt = OffsetDateTime.now();
    }

    public boolean isOpen() { return releasedAt == null; }

    public SoftwareLicense getLicense() { return license; }
    public Person getPerson() { return person; }
    public Asset getAsset() { return asset; }
    public OffsetDateTime getAssignedAt() { return assignedAt; }
    public OffsetDateTime getReleasedAt() { return releasedAt; }
    public String getNotes() { return notes; }
}
