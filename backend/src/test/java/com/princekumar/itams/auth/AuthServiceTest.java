package com.princekumar.itams.auth;

import com.princekumar.itams.auth.dto.LoginRequest;
import com.princekumar.itams.auth.dto.TokenResponse;
import com.princekumar.itams.person.Person;
import com.princekumar.itams.user.RefreshToken;
import com.princekumar.itams.user.RefreshTokenRepository;
import com.princekumar.itams.user.UserAccount;
import com.princekumar.itams.user.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Guards the "no username enumeration" property of {@code AuthService.login()}.
 * Regardless of the reason a login fails (wrong user, wrong password, disabled
 * account), the caller must get the same {@link BadCredentialsException} and
 * therefore the same 401 {@code auth.bad_credentials} response.
 *
 * <p>Also covers refresh-token rotation and reuse detection (token families).</p>
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserAccountRepository userRepo;
    @Mock RefreshTokenRepository refreshRepo;
    @Mock PasswordEncoder encoder;
    @Mock JwtService jwt;
    @InjectMocks AuthService service;

    @Test
    void login_with_unknown_user_throws_bad_credentials() {
        when(userRepo.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(new LoginRequest("ghost", "whatever")))
            .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void login_with_wrong_password_throws_bad_credentials() {
        UserAccount user = enabledUser("alex", "hash");
        when(userRepo.findByUsername("alex")).thenReturn(Optional.of(user));
        when(encoder.matches("nope", "hash")).thenReturn(false);

        assertThatThrownBy(() -> service.login(new LoginRequest("alex", "nope")))
            .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void login_with_disabled_account_throws_bad_credentials_not_disabled() {
        UserAccount user = enabledUser("alex", "hash");
        user.setEnabled(false);
        when(userRepo.findByUsername("alex")).thenReturn(Optional.of(user));

        // Critical: NOT DisabledException — that would leak "this username exists".
        assertThatThrownBy(() -> service.login(new LoginRequest("alex", "any")))
            .isInstanceOf(BadCredentialsException.class);
    }

    // ── refresh: rotation + reuse detection ───────────────────────────────────

    @Test
    void refresh_rotates_within_the_same_family() {
        UUID family = UUID.randomUUID();
        RefreshToken current = token(enabledUser("alex", "hash"), "h-old", family);
        when(jwt.hashRefreshToken("raw-old")).thenReturn("h-old");
        when(refreshRepo.findByTokenHash("h-old")).thenReturn(Optional.of(current));
        when(jwt.createAccessToken(anyString(), any(), any())).thenReturn("access");
        when(jwt.generateRefreshTokenRaw()).thenReturn("raw-new");
        when(jwt.hashRefreshToken("raw-new")).thenReturn("h-new");
        when(jwt.refreshTokenTtl()).thenReturn(Duration.ofDays(7));
        when(jwt.accessTokenTtlSeconds()).thenReturn(900L);

        TokenResponse fresh = service.refresh("raw-old");

        assertThat(fresh.refreshToken()).isEqualTo("raw-new");
        ArgumentCaptor<RefreshToken> saved = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshRepo).save(saved.capture());
        assertThat(saved.getValue().getFamilyId()).isEqualTo(family);   // same login chain
        assertThat(current.wasRotated()).isTrue();
        assertThat(current.getReplacedByTokenHash()).isEqualTo("h-new");
    }

    @Test
    void refresh_with_already_rotated_token_revokes_the_whole_family() {
        UUID family = UUID.randomUUID();
        RefreshToken stolen = token(enabledUser("alex", "hash"), "h-old", family);
        stolen.revoke("h-new");                       // it was rotated before
        when(jwt.hashRefreshToken("raw-old")).thenReturn("h-old");
        when(refreshRepo.findByTokenHash("h-old")).thenReturn(Optional.of(stolen));
        when(refreshRepo.revokeActiveInFamily(eq(family), any())).thenReturn(1);

        assertThatThrownBy(() -> service.refresh("raw-old"))
            .isInstanceOf(BadCredentialsException.class);

        verify(refreshRepo).revokeActiveInFamily(eq(family), any());
        verify(refreshRepo, never()).save(any());     // no new tokens for a replay
    }

    @Test
    void refresh_with_logged_out_token_is_rejected_without_family_revocation() {
        RefreshToken loggedOut = token(enabledUser("alex", "hash"), "h-old", UUID.randomUUID());
        loggedOut.revoke(null);                       // logout, not rotation
        when(jwt.hashRefreshToken("raw-old")).thenReturn("h-old");
        when(refreshRepo.findByTokenHash("h-old")).thenReturn(Optional.of(loggedOut));

        assertThatThrownBy(() -> service.refresh("raw-old"))
            .isInstanceOf(BadCredentialsException.class);

        verify(refreshRepo, never()).revokeActiveInFamily(any(), any());
    }

    private static RefreshToken token(UserAccount user, String hash, UUID family) {
        OffsetDateTime now = OffsetDateTime.now();
        return new RefreshToken(user, hash, family, now.minusMinutes(5), now.plusDays(7));
    }

    private static UserAccount enabledUser(String username, String passwordHash) {
        Person p = new Person("Alex", "K", "alex@example.com", null);
        return new UserAccount(p, username, passwordHash);
    }
}
