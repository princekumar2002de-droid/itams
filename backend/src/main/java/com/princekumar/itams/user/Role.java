package com.princekumar.itams.user;

import jakarta.persistence.*;

import java.util.Objects;

/**
 * A role in the system: ADMIN, IT_MANAGER, EMPLOYEE.
 *
 * <p>Deliberately does <b>not</b> extend {@code BaseEntity} because the DB
 * table has no {@code created_at}/{@code updated_at} columns — roles are
 * static reference data seeded in V2.</p>
 */
@Entity
@Table(name = "role")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30, unique = true)
    private String code;

    @Column(length = 255)
    private String description;

    protected Role() { /* JPA */ }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getDescription() { return description; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Role r)) return false;
        return id != null && id.equals(r.id);
    }
    @Override public int hashCode() { return Objects.hashCode(getClass()); }
}
