package com.princekumar.itams.assignment;

import com.princekumar.itams.asset.Asset;
import com.princekumar.itams.common.entity.BaseEntity;
import com.princekumar.itams.person.Person;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * The link between one {@link Asset} and one {@link Person} for a period
 * of time.
 *
 * <p>An asset can have <b>at most one open assignment at any time</b> —
 * this is enforced at the DB level by a partial unique index
 * ({@code ux_asset_assignment_one_open}). The service layer additionally
 * checks it inside a {@code SELECT ... FOR UPDATE} to give clean 409
 * responses instead of raw constraint-violation exceptions.</p>
 *
 * <p>{@code assignedByUserId} and {@code returnedByUserId} reference
 * {@code user_account.id} and are taken from the logged-in user
 * ({@link com.princekumar.itams.auth.CurrentUser}).</p>
 */
@Entity
@Table(name = "asset_assignment")
public class AssetAssignment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false, updatable = false)
    private Asset asset;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assignee_person_id", nullable = false, updatable = false)
    private Person assignee;

    @Column(name = "assigned_by_user_id", nullable = false, updatable = false)
    private Long assignedByUserId;

    @Column(name = "assigned_at", nullable = false, updatable = false)
    private OffsetDateTime assignedAt;

    @Column(name = "expected_return_on")
    private LocalDate expectedReturnOn;

    @Column(name = "actual_return_at")
    private OffsetDateTime actualReturnAt;

    @Column(name = "returned_by_user_id")
    private Long returnedByUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "out_condition", nullable = false, length = 10, updatable = false)
    private AssetCondition outCondition;

    @Enumerated(EnumType.STRING)
    @Column(name = "in_condition", length = 10)
    private AssetCondition inCondition;

    @Column(columnDefinition = "text")
    private String notes;

    protected AssetAssignment() { /* JPA */ }

    public AssetAssignment(Asset asset, Person assignee, Long assignedByUserId,
                           LocalDate expectedReturnOn, AssetCondition outCondition, String notes) {
        this.asset = asset;
        this.assignee = assignee;
        this.assignedByUserId = assignedByUserId;
        this.assignedAt = OffsetDateTime.now();
        this.expectedReturnOn = expectedReturnOn;
        this.outCondition = outCondition;
        this.notes = notes;
    }

    /** Closes the assignment. Caller is responsible for transitioning the asset. */
    void closeReturn(Long returnedByUserId, AssetCondition inCondition, String notesAppend) {
        if (this.actualReturnAt != null) {
            throw new IllegalStateException("Assignment already returned");
        }
        this.actualReturnAt = OffsetDateTime.now();
        this.returnedByUserId = returnedByUserId;
        this.inCondition = inCondition;
        if (notesAppend != null && !notesAppend.isBlank()) {
            this.notes = (this.notes == null ? "" : this.notes + "\n") + "[return] " + notesAppend;
        }
    }

    public boolean isOpen() { return actualReturnAt == null; }

    public Asset getAsset() { return asset; }
    public Person getAssignee() { return assignee; }
    public Long getAssignedByUserId() { return assignedByUserId; }
    public OffsetDateTime getAssignedAt() { return assignedAt; }
    public LocalDate getExpectedReturnOn() { return expectedReturnOn; }
    public OffsetDateTime getActualReturnAt() { return actualReturnAt; }
    public Long getReturnedByUserId() { return returnedByUserId; }
    public AssetCondition getOutCondition() { return outCondition; }
    public AssetCondition getInCondition() { return inCondition; }
    public String getNotes() { return notes; }
}
