package com.princekumar.itams.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * All token operations in one place: create access JWTs, verify them,
 * generate opaque refresh tokens, and hash them for storage.
 */
@Service
public class JwtService {

    private final JwtProperties props;
    private SecretKey key;

    public JwtService(JwtProperties props) {
        this.props = props;
    }

    @PostConstruct
    void init() {
        if (props.secret() == null || props.secret().isBlank()) {
            throw new IllegalStateException(
                "JWT_SECRET is not set. Add it to docker/.env (see .env.example) — the backend refuses to start without it.");
        }
        byte[] bytes = props.secret().getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException(
                "JWT_SECRET must be at least 32 bytes (256 bits) for HS256. Got " + bytes.length + " bytes.");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
    }

    // ── access token ─────────────────────────────────────────────────────────

    public String createAccessToken(String username, Long userId, Collection<? extends GrantedAuthority> authorities) {
        Instant now = Instant.now();
        Instant exp = now.plus(Duration.ofMinutes(props.accessTokenTtlMinutes()));
        List<String> roles = authorities.stream().map(GrantedAuthority::getAuthority).toList();

        return Jwts.builder()
            .issuer(props.issuer())
            .subject(username)
            .issuedAt(Date.from(now))
            .expiration(Date.from(exp))
            .claim("uid", userId)
            .claim("roles", roles)
            .signWith(key, Jwts.SIG.HS256)
            .compact();
    }

    public long accessTokenTtlSeconds() {
        return Duration.ofMinutes(props.accessTokenTtlMinutes()).toSeconds();
    }

    /** Throws {@link JwtException} on any invalid token — caller decides how to react. */
    public Claims parseAccessToken(String token) throws JwtException {
        return Jwts.parser()
            .verifyWith(key)
            .requireIssuer(props.issuer())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    // ── refresh token ────────────────────────────────────────────────────────

    /** 512 bits of secure randomness, Base64URL-encoded (no padding). */
    public String generateRefreshTokenRaw() {
        byte[] bytes = new byte[64];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * SHA-256 of the raw refresh token, Base64URL-encoded. We store the hash,
     * not the raw token — if the DB is leaked, existing refresh tokens still
     * aren't usable.
     */
    public String hashRefreshToken(String rawToken) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] hash = sha256.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required by every JVM", e);
        }
    }

    public Duration refreshTokenTtl() {
        return Duration.ofDays(props.refreshTokenTtlDays());
    }
}
