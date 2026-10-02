package com.princekumar.itams.user;

import com.princekumar.itams.common.entity.BaseEntity;
import com.princekumar.itams.person.Person;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * A login for a {@link Person}. 1:1 with Person.
 *
 * <p>Roles are attached through the {@code user_role} join table
 * (M:N for flexibility, although today every user has exactly one role
 * from {@code ADMIN | IT_MANAGER | EMPLOYEE}).</p>
 */
@Entity
@Table(name = "user_account")
public class UserAccount extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "person_id", nullable = false, unique = true, updatable = false)
    private Person person;

    @Column(nullable = false, length = 80, unique = true, updatable = false)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "last_login_at")
    private OffsetDateTime lastLoginAt;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_role",
        joinColumns = @JoinColumn(name = "user_account_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    protected UserAccount() { /* JPA */ }

    public UserAccount(Person person, String username, String passwordHash) {
        this.person = person;
        this.username = username;
        this.passwordHash = passwordHash;
    }

    public void addRole(Role role) { this.roles.add(role); }
    public void touchLastLogin()   { this.lastLoginAt = OffsetDateTime.now(); }
    public void setPasswordHash(String v) { this.passwordHash = v; }
    public void setEnabled(boolean v)     { this.enabled = v; }

    public Person getPerson() { return person; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public boolean isEnabled() { return enabled; }
    public OffsetDateTime getLastLoginAt() { return lastLoginAt; }
    public Set<Role> getRoles() { return roles; }
}
