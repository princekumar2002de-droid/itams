package com.princekumar.itams.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Bound to the {@code jwt.*} config in {@code application.yml}.
 *
 * @param secret                   HMAC secret ≥ 32 chars (256 bits for HS256).
 *                                 REQUIRED — the app fails to start if empty.
 * @param accessTokenTtlMinutes    lifetime of the short access token
 * @param refreshTokenTtlDays      lifetime of the refresh token
 * @param issuer                   {@code iss} claim baked into every token
 */
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
    String secret,
    long accessTokenTtlMinutes,
    long refreshTokenTtlDays,
    String issuer
) {}
