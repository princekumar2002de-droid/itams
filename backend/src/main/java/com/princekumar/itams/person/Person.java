package com.princekumar.itams.person;

import com.princekumar.itams.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/**
 * A human — the base identity for employees, users, and asset assignees.
 *
 * <p>A person may have zero or one linked {@code Employee}, and zero or
 * one linked {@code UserAccount}. Splitting these lets us represent
 * contractors (asset assignee, no login) and system accounts (login, no
 * employment) without nullable columns everywhere.</p>
 */
@Entity
@Table(name = "person")
public class Person extends BaseEntity {

    @Column(name = "first_name", nullable = false, length = 80)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 80)
    private String lastName;

    @Column(nullable = false, length = 160)
    private String email;

    @Column(length = 40)
    private String phone;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    protected Person() { /* JPA */ }

    public Person(String firstName, String lastName, String email, String phone) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
    }

    public void softDelete() {
        if (this.deletedAt == null) this.deletedAt = OffsetDateTime.now();
    }

    public boolean isDeleted() { return deletedAt != null; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String v) { this.firstName = v; }
    public String getLastName() { return lastName; }
    public void setLastName(String v) { this.lastName = v; }
    public String getEmail() { return email; }
    public void setEmail(String v) { this.email = v; }
    public String getPhone() { return phone; }
    public void setPhone(String v) { this.phone = v; }
    public boolean isActive() { return active; }
    public void setActive(boolean v) { this.active = v; }
    public OffsetDateTime getDeletedAt() { return deletedAt; }
}
