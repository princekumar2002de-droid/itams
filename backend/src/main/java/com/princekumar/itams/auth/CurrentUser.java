package com.princekumar.itams.auth;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Small helper to read the currently authenticated user from the
 * {@link SecurityContextHolder}. Kept static-only so services can call
 * it without extra dependency injection.
 *
 * <p>Falls back to id=1 (the seeded system bootstrap user) when no
 * authentication is present — that's the situation in unit tests that
 * don't wire up security. For {@link #personId()} the fallback is also
 * 1, i.e. the seeded "System Bootstrap" person.</p>
 *
 * <p>{@link #personId()}, {@link #rolesOf(Authentication)} and
 * {@link #hasOnlyRole(String)} support binding the ticket reporter on the server
 * and the row-level scoping for the EMPLOYEE role.
 * See {@link com.princekumar.itams.ticket.TicketService} and SECURITY.md.</p>
 */
public final class CurrentUser {
    private CurrentUser() {}

    private static final long SYSTEM_USER_ID   = 1L;
    private static final long SYSTEM_PERSON_ID = 1L;

    /** Never returns null; callers can safely use it as a FK value. */
    public static long id() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return SYSTEM_USER_ID;
        if (auth.getPrincipal() instanceof AppUserDetails aud) {
            return aud.getUserId();
        }
        return SYSTEM_USER_ID;
    }

    /** The {@code person_id} of the authenticated user, or the bootstrap system person. */
    public static long personId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return SYSTEM_PERSON_ID;
        if (auth.getPrincipal() instanceof AppUserDetails aud) {
            Long p = aud.getPersonId();
            return p != null ? p : SYSTEM_PERSON_ID;
        }
        return SYSTEM_PERSON_ID;
    }

    /**
     * Role codes (without the {@code ROLE_} prefix) held by the current user.
     * Returns an empty set when no authentication is present.
     */
    public static Set<String> roles() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return Set.of();
        return rolesOf(auth);
    }

    /**
     * True iff the current user has exactly one role and that role matches.
     * Used to apply row-level ownership scoping only to users whose authority
     * is purely a given role (e.g. an EMPLOYEE who is also IT_MANAGER sees
     * everything the IT_MANAGER sees, not the restricted EMPLOYEE slice).
     */
    public static boolean hasOnlyRole(String role) {
        Set<String> r = roles();
        return r.size() == 1 && r.contains(role);
    }

    /** Visible for testing. */
    static Set<String> rolesOf(Authentication auth) {
        return auth.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .map(a -> a.startsWith("ROLE_") ? a.substring(5) : a)
            .collect(Collectors.toSet());
    }
}
