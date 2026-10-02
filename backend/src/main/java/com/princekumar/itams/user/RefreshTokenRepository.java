package com.princekumar.itams.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Revoke every still-active token of one login family in a single UPDATE.
     * Used when a rotated token is replayed (probable theft).
     *
     * @return number of tokens revoked
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
           update RefreshToken t
              set t.revokedAt = :now
            where t.familyId = :familyId
              and t.revokedAt is null
           """)
    int revokeActiveInFamily(@Param("familyId") UUID familyId, @Param("now") OffsetDateTime now);
}
