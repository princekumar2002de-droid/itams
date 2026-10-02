package com.princekumar.itams.department;

import com.princekumar.itams.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/**
 * A department in the organisational tree.
 *
 * <p>Notes on the mapping:</p>
 * <ul>
 *   <li>{@code parentDepartment} is a self-referencing {@code @ManyToOne}
 *       so we can navigate the hierarchy in-object.</li>
 *   <li>{@code managerPersonId} is a plain {@code Long} rather than a
 *       {@code @ManyToOne} to {@code Person}. The department module stays
 *       independent of the person module; the database foreign key still
 *       guarantees the id is valid.</li>
 *   <li>Soft-delete via {@code deletedAt}. The service is what enforces
 *       "hide soft-deleted rows from list queries"; the DB still stores
 *       them for the audit trail.</li>
 * </ul>
 */
@Entity
@Table(name = "department")
public class Department extends BaseEntity {

    @Column(nullable = false, length = 30)
    private String code;

    @Column(nullable = false, length = 160)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_department_id")
    private Department parentDepartment;

    @Column(name = "manager_person_id")
    private Long managerPersonId;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    protected Department() { /* JPA */ }

    public Department(String code, String name, Department parentDepartment, Long managerPersonId) {
        this.code = code;
        this.name = name;
        this.parentDepartment = parentDepartment;
        this.managerPersonId = managerPersonId;
    }

    // ── behaviour ────────────────────────────────────────────────────────────

    /** Marks the row soft-deleted. Idempotent. */
    public void softDelete() {
        if (this.deletedAt == null) {
            this.deletedAt = OffsetDateTime.now();
        }
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    // ── getters / setters ────────────────────────────────────────────────────

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Department getParentDepartment() { return parentDepartment; }
    public void setParentDepartment(Department parentDepartment) { this.parentDepartment = parentDepartment; }

    public Long getManagerPersonId() { return managerPersonId; }
    public void setManagerPersonId(Long managerPersonId) { this.managerPersonId = managerPersonId; }

    public OffsetDateTime getDeletedAt() { return deletedAt; }
}
