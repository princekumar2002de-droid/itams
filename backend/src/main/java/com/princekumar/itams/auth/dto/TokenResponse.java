package com.princekumar.itams.auth.dto;

public record TokenResponse(
    String accessToken,
    String refreshToken,
    String tokenType,       // always "Bearer"
    long   expiresIn        // access-token TTL in seconds
) {}
