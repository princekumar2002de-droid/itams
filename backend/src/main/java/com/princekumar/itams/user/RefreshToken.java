package com.princekumar.itams.user;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Server-side record of an issued refresh token.
 *
 * <p>We do <b>not</b> store the raw token — we store a SHA-256 hash of it.
 * If the DB is ever leaked, the tokens themselves aren't usable.</p>
 *
 * <p>Rotation: when a refresh token is used, we mark it revoked with
 * {@code replaced_by_token_hash} pointing at the new one. All tokens produced
 * by one login share a {@code family_id}. Presenting an already-rotated token
 * again is treated as theft and revokes the whole family (see
 * {@code AuthService.refresh}).</p>
 */
@Entity
@Table(name = "refresh_token")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_account_id", nullable = false, updatable = false)
    private UserAccount userAccount;

    @Column(name = "token_hash", nullable = false, length = 255, unique = true, updatable = false)
    private String tokenHash;

    @Column(name = "issued_at", nullable = false, updatable = false)
    private OffsetDateTime issuedAt;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;

    @Column(name = "replaced_by_token_hash", length = 255)
    private String replacedByTokenHash;

    /** Shared by every token descended from one login. */
    @Column(name = "family_id", nullable = false, updatable = false)
    private UUID familyId;

    protected RefreshToken() { /* JPA */ }

    public RefreshToken(UserAccount userAccount, String tokenHash, UUID familyId,
                        OffsetDateTime issuedAt, OffsetDateTime expiresAt) {
        this.userAccount = userAccount;
        this.familyId = familyId;
        this.tokenHash = tokenHash;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
    }

    public boolean isRevoked() { return revokedAt != null; }
    public boolean isExpired() { return expiresAt.isBefore(OffsetDateTime.now()); }
    public boolean isUsable() { return !isRevoked() && !isExpired(); }

    /** Revoked because it was exchanged for a newer token (as opposed to logout). */
    public boolean wasRotated() { return isRevoked() && replacedByTokenHash != null; }

    public void revoke(String replacedBy) {
        this.revokedAt = OffsetDateTime.now();
        this.replacedByTokenHash = replacedBy;
    }

    public Long getId() { return id; }
    public UserAccount getUserAccount() { return userAccount; }
    public String getTokenHash() { return tokenHash; }
    public OffsetDateTime getIssuedAt() { return issuedAt; }
    public OffsetDateTime getExpiresAt() { return expiresAt; }
    public OffsetDateTime getRevokedAt() { return revokedAt; }
    public String getReplacedByTokenHash() { return replacedByTokenHash; }
    public UUID getFamilyId() { return familyId; }
}
