package com.princekumar.itams.auth.dto;

import java.util.List;

public record MeResponse(
    Long userId,
    String username,
    Long personId,
    String firstName,
    String lastName,
    String email,
    List<String> roles
) {}
