package com.princekumar.itams.auth;

import com.princekumar.itams.person.Person;
import com.princekumar.itams.user.Role;
import com.princekumar.itams.user.UserAccount;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the role and person helpers in {@link CurrentUser}:
 * {@link CurrentUser#personId()}, {@link CurrentUser#roles()},
 * {@link CurrentUser#hasOnlyRole(String)}.
 */
class CurrentUserTest {

    @AfterEach
    void clearAuth() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void id_and_personId_fall_back_to_system_when_no_auth() {
        SecurityContextHolder.clearContext();
        assertThat(CurrentUser.id()).isEqualTo(1L);
        assertThat(CurrentUser.personId()).isEqualTo(1L);
        assertThat(CurrentUser.roles()).isEmpty();
    }

    @Test
    void hasOnlyRole_true_for_single_role_user() {
        authenticate(10L, 20L, "EMPLOYEE");
        assertThat(CurrentUser.hasOnlyRole("EMPLOYEE")).isTrue();
        assertThat(CurrentUser.hasOnlyRole("ADMIN")).isFalse();
    }

    @Test
    void hasOnlyRole_false_when_user_has_multiple_roles() {
        authenticate(10L, 20L, "EMPLOYEE", "IT_MANAGER");
        // EMPLOYEE is held but not the ONLY role, so hasOnlyRole returns false
        // — an EMPLOYEE+IT_MANAGER should see what IT_MANAGER sees, not the EMPLOYEE slice.
        assertThat(CurrentUser.hasOnlyRole("EMPLOYEE")).isFalse();
        assertThat(CurrentUser.hasOnlyRole("IT_MANAGER")).isFalse();
    }

    @Test
    void personId_returns_the_authenticated_persons_id() {
        authenticate(10L, 42L, "ADMIN");
        assertThat(CurrentUser.personId()).isEqualTo(42L);
        assertThat(CurrentUser.id()).isEqualTo(10L);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private static void authenticate(long userId, long personId, String... roleCodes) {
        Person person = new Person("U", "ser", "u@x", null);
        setField(person, "id", personId);
        UserAccount account = new UserAccount(person, "u" + userId, "x");
        setField(account, "id", userId);
        try {
            var ctor = Role.class.getDeclaredConstructor();
            ctor.setAccessible(true);
            for (String rc : roleCodes) {
                Role r = ctor.newInstance();
                setField(r, "code", rc);
                setField(r, "id", (long) rc.hashCode());
                account.addRole(r);
            }
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
        AppUserDetails principal = new AppUserDetails(account);
        var auth = new UsernamePasswordAuthenticationToken(principal, "x", principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private static void setField(Object target, String name, Object value) {
        Class<?> c = target.getClass();
        while (c != null) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                f.set(target, value);
                return;
            } catch (NoSuchFieldException ignored) {
                c = c.getSuperclass();
            } catch (IllegalAccessException e) {
                throw new AssertionError(e);
            }
        }
        throw new AssertionError("no field '" + name + "' on " + target.getClass());
    }
}
