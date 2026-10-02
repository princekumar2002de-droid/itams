package com.princekumar.itams.auth;

import com.princekumar.itams.auth.dto.LoginRequest;
import com.princekumar.itams.auth.dto.LogoutRequest;
import com.princekumar.itams.auth.dto.MeResponse;
import com.princekumar.itams.auth.dto.RefreshRequest;
import com.princekumar.itams.auth.dto.TokenResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Login, refresh, logout, and current-user lookup")
public class AuthController {

    private final AuthService service;

    public AuthController(AuthService service) { this.service = service; }

    @Operation(summary = "Log in with username + password")
    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest req) {
        return service.login(req);
    }

    @Operation(summary = "Rotate an unexpired refresh token into a new pair")
    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest req) {
        return service.refresh(req.refreshToken());
    }

    @Operation(summary = "Revoke a refresh token (idempotent)")
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody LogoutRequest req) {
        service.logout(req.refreshToken());
    }

    @Operation(summary = "Current authenticated user")
    @GetMapping("/me")
    public MeResponse me(Authentication auth) {
        return service.me(auth.getName());
    }
}
