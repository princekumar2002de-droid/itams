package com.princekumar.itams.auth;

import com.princekumar.itams.auth.dto.LoginRequest;
import com.princekumar.itams.auth.dto.MeResponse;
import com.princekumar.itams.auth.dto.TokenResponse;
import com.princekumar.itams.common.exception.BusinessRuleViolationException;
import com.princekumar.itams.user.RefreshToken;
import com.princekumar.itams.user.RefreshTokenRepository;
import com.princekumar.itams.user.UserAccount;
import com.princekumar.itams.user.UserAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Login, refresh, logout, and current-user lookup.
 *
 * <p>All password-verification and token-issuing lives here so controllers
 * stay HTTP-shaped and unit tests don't need MockMvc.</p>
 */
@Service
@Transactional
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserAccountRepository userRepo;
    private final RefreshTokenRepository refreshRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwt;

    public AuthService(UserAccountRepository userRepo,
                       RefreshTokenRepository refreshRepo,
                       PasswordEncoder passwordEncoder,
                       JwtService jwt) {
        this.userRepo = userRepo;
        this.refreshRepo = refreshRepo;
        this.passwordEncoder = passwordEncoder;
        this.jwt = jwt;
    }

    // ── login ────────────────────────────────────────────────────────────────

    public TokenResponse login(LoginRequest req) {
        UserAccount user = userRepo.findByUsername(req.username())
            .orElseThrow(() -> new BadCredentialsException("Bad credentials"));
        // Deliberately return the SAME error for wrong-password AND disabled-account.
        // Any distinguishable response here (e.g. DisabledException → 403) would let
        // an attacker enumerate valid usernames by watching the response code.
        if (!user.isEnabled() || !passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Bad credentials");
        }
        user.touchLastLogin();
        return issueTokens(user, UUID.randomUUID());   // new login = new token family
    }

    // ── refresh ──────────────────────────────────────────────────────────────

    /**
     * Exchange a refresh token for a new pair (rotation).
     *
     * <p><b>Reuse detection:</b> if the presented token was already rotated, someone is
     * replaying an old token — either an attacker who stole it, or the real user after
     * the attacker already used it. We can't tell which, so we revoke every active token
     * in that login's family and force a fresh login. Other sessions are unaffected.</p>
     *
     * <p>{@code noRollbackFor}: the revocation must be committed even though we then throw
     * {@link BadCredentialsException}. Without it, Spring would roll the revocation back
     * along with the failed request and the stolen chain would stay alive.</p>
     */
    @Transactional(noRollbackFor = BadCredentialsException.class)
    public TokenResponse refresh(String rawRefreshToken) {
        String hash = jwt.hashRefreshToken(rawRefreshToken);
        RefreshToken existing = refreshRepo.findByTokenHash(hash)
            .orElseThrow(() -> new BadCredentialsException("Bad refresh token"));

        if (existing.wasRotated()) {
            int revoked = refreshRepo.revokeActiveInFamily(existing.getFamilyId(), OffsetDateTime.now());
            log.warn("Refresh-token reuse detected for user id={} family={} — revoked {} active token(s)",
                existing.getUserAccount().getId(), existing.getFamilyId(), revoked);
            throw new BadCredentialsException("Refresh token reuse detected");
        }
        if (!existing.isUsable()) {
            throw new BadCredentialsException("Refresh token revoked or expired");
        }

        UserAccount user = existing.getUserAccount();
        if (!user.isEnabled()) {
            throw new DisabledException("Account is disabled");
        }

        // Rotate: issue new pair, mark old as replaced.
        TokenResponse fresh = issueTokens(user, existing.getFamilyId());
        String newHash = jwt.hashRefreshToken(fresh.refreshToken());
        existing.revoke(newHash);
        return fresh;
    }

    // ── logout ───────────────────────────────────────────────────────────────

    public void logout(String rawRefreshToken) {
        String hash = jwt.hashRefreshToken(rawRefreshToken);
        refreshRepo.findByTokenHash(hash).ifPresent(rt -> rt.revoke(null));
        // Idempotent: unknown token still returns 204.
    }

    // ── /me ──────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public MeResponse me(String username) {
        UserAccount user = userRepo.findByUsername(username)
            .orElseThrow(() -> new BusinessRuleViolationException(
                "auth.stale_token", "Token references a user that no longer exists."));
        var p = user.getPerson();
        List<String> roles = user.getRoles().stream().map(r -> "ROLE_" + r.getCode()).toList();
        return new MeResponse(
            user.getId(), user.getUsername(),
            p.getId(), p.getFirstName(), p.getLastName(), p.getEmail(),
            roles
        );
    }

    // ── helper ───────────────────────────────────────────────────────────────

    private TokenResponse issueTokens(UserAccount user, UUID familyId) {
        var authorities = user.getRoles().stream()
            .map(r -> new SimpleGrantedAuthority("ROLE_" + r.getCode()))
            .toList();
        String access = jwt.createAccessToken(user.getUsername(), user.getId(), authorities);

        String rawRefresh = jwt.generateRefreshTokenRaw();
        String refreshHash = jwt.hashRefreshToken(rawRefresh);
        OffsetDateTime issued = OffsetDateTime.now();
        OffsetDateTime expires = issued.plus(jwt.refreshTokenTtl());
        refreshRepo.save(new RefreshToken(user, refreshHash, familyId, issued, expires));

        return new TokenResponse(access, rawRefresh, "Bearer", jwt.accessTokenTtlSeconds());
    }
}
