package com.princekumar.itams.ticket;

import com.princekumar.itams.asset.AssetRepository;
import com.princekumar.itams.auth.AppUserDetails;
import com.princekumar.itams.common.exception.ResourceNotFoundException;
import com.princekumar.itams.person.Person;
import com.princekumar.itams.person.PersonRepository;
import com.princekumar.itams.ticket.dto.CreateTicketRequest;
import com.princekumar.itams.user.Role;
import com.princekumar.itams.user.UserAccount;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the EMPLOYEE row-level scoping in {@link TicketService}:
 * <ul>
 *   <li>{@code POST /tickets} — EMPLOYEE-only caller cannot set a reporterPersonId that isn't their own.</li>
 *   <li>{@code GET /tickets} — EMPLOYEE-only caller only sees their own tickets.</li>
 *   <li>{@code GET /tickets/{id}} — EMPLOYEE-only caller gets 404 on another employee's ticket.</li>
 * </ul>
 *
 * The {@code SecurityContextHolder} is populated per-test to drive
 * {@link com.princekumar.itams.auth.CurrentUser}.
 */
@ExtendWith(MockitoExtension.class)
class TicketOwnershipScopingTest {

    @Mock TicketRepository repo;
    @Mock TicketCommentRepository commentRepo;
    @Mock PersonRepository personRepo;
    @Mock AssetRepository assetRepo;
    @InjectMocks TicketService service;

    private static final long ALICE_USER_ID = 10L;
    private static final long ALICE_PERSON_ID = 20L;
    private static final long BOB_PERSON_ID = 30L;

    @AfterEach
    void clearAuth() {
        SecurityContextHolder.clearContext();
    }

    // ── create ───────────────────────────────────────────────────────────────

    @Test
    void employee_create_coerces_reporter_to_self_even_when_body_lies() {
        authenticateAsEmployee(ALICE_USER_ID, ALICE_PERSON_ID);
        Person alice = personWithId(ALICE_PERSON_ID);
        when(personRepo.findByIdAndDeletedAtIsNull(ALICE_PERSON_ID)).thenReturn(Optional.of(alice));
        when(repo.save(any(Ticket.class))).thenAnswer(inv -> {
            Ticket t = inv.getArgument(0);
            setId(t, 101L);
            return t;
        });

        var requestForgingBobAsReporter =
            new CreateTicketRequest("Loud fan", "...", TicketPriority.LOW, BOB_PERSON_ID, null);

        Ticket created = service.create(requestForgingBobAsReporter);

        // Reporter is Alice, not Bob — the service coerced it.
        assertThat(created.getReporter()).isSameAs(alice);
        verify(personRepo).findByIdAndDeletedAtIsNull(ALICE_PERSON_ID);
    }

    @Test
    void admin_create_honours_client_supplied_reporter() {
        authenticateAsAdmin();
        Person bob = personWithId(BOB_PERSON_ID);
        when(personRepo.findByIdAndDeletedAtIsNull(BOB_PERSON_ID)).thenReturn(Optional.of(bob));
        when(repo.save(any(Ticket.class))).thenAnswer(inv -> {
            Ticket t = inv.getArgument(0);
            setId(t, 102L);
            return t;
        });

        var request = new CreateTicketRequest("On behalf of Bob", "...",
            TicketPriority.LOW, BOB_PERSON_ID, null);

        Ticket created = service.create(request);

        assertThat(created.getReporter()).isSameAs(bob);
    }

    // ── list ─────────────────────────────────────────────────────────────────

    @Test
    void employee_search_overrides_reporterPersonId_filter_to_self() {
        authenticateAsEmployee(ALICE_USER_ID, ALICE_PERSON_ID);
        Pageable p = PageRequest.of(0, 20);
        Page<Ticket> empty = new PageImpl<>(List.of(), p, 0);
        when(repo.search(isNull(), isNull(), isNull(), isNull(), eq(ALICE_PERSON_ID), eq(p)))
            .thenReturn(empty);

        // Client tries to query Bob's tickets — service rewrites the filter to Alice.
        service.search(null, null, null, null, BOB_PERSON_ID, p);

        verify(repo).search(isNull(), isNull(), isNull(), isNull(), eq(ALICE_PERSON_ID), eq(p));
    }

    @Test
    void admin_search_passes_reporterPersonId_through_unchanged() {
        authenticateAsAdmin();
        Pageable p = PageRequest.of(0, 20);
        Page<Ticket> empty = new PageImpl<>(List.of(), p, 0);
        when(repo.search(isNull(), isNull(), isNull(), isNull(), eq(BOB_PERSON_ID), eq(p)))
            .thenReturn(empty);

        service.search(null, null, null, null, BOB_PERSON_ID, p);

        verify(repo).search(isNull(), isNull(), isNull(), isNull(), eq(BOB_PERSON_ID), eq(p));
    }

    // ── get by id ────────────────────────────────────────────────────────────

    @Test
    void employee_findById_404_for_other_employees_ticket() {
        authenticateAsEmployee(ALICE_USER_ID, ALICE_PERSON_ID);
        Ticket bobsTicket = ticketReportedBy(BOB_PERSON_ID, 555L);
        when(repo.findById(555L)).thenReturn(Optional.of(bobsTicket));

        assertThatThrownBy(() -> service.findById(555L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("555");
    }

    @Test
    void employee_findById_ok_for_own_ticket() {
        authenticateAsEmployee(ALICE_USER_ID, ALICE_PERSON_ID);
        Ticket mine = ticketReportedBy(ALICE_PERSON_ID, 777L);
        when(repo.findById(777L)).thenReturn(Optional.of(mine));

        Ticket found = service.findById(777L);
        assertThat(found).isSameAs(mine);
    }

    @Test
    void employee_findById_ok_when_assigned_to_me_even_if_reporter_is_someone_else() {
        authenticateAsEmployee(ALICE_USER_ID, ALICE_PERSON_ID);
        Ticket t = ticketReportedBy(BOB_PERSON_ID, 888L);
        t.setAssignedToUserId(ALICE_USER_ID);
        when(repo.findById(888L)).thenReturn(Optional.of(t));

        Ticket found = service.findById(888L);
        assertThat(found).isSameAs(t);
    }

    @Test
    void admin_findById_sees_any_ticket() {
        authenticateAsAdmin();
        Ticket bobsTicket = ticketReportedBy(BOB_PERSON_ID, 999L);
        when(repo.findById(999L)).thenReturn(Optional.of(bobsTicket));

        Ticket found = service.findById(999L);
        assertThat(found).isSameAs(bobsTicket);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private static Person personWithId(long id) {
        Person p = new Person("First", "Last", "mail@example.com", null);
        setId(p, id);
        return p;
    }

    private static Ticket ticketReportedBy(long reporterPersonId, long ticketId) {
        Person reporter = personWithId(reporterPersonId);
        Ticket t = new Ticket("subj", "desc", TicketPriority.LOW, reporter, null);
        setId(t, ticketId);
        return t;
    }

    private static void authenticateAsEmployee(long userId, long personId) {
        authenticate(userId, personId, "EMPLOYEE");
    }

    private static void authenticateAsAdmin() {
        authenticate(999L, 999L, "ADMIN");
    }

    private static void authenticate(long userId, long personId, String... roleCodes) {
        Person person = new Person("U", "ser", "u@x", null);
        setId(person, personId);
        UserAccount account = new UserAccount(person, "u" + userId, "x");
        setId(account, userId);
        for (String rc : roleCodes) {
            Role r = buildRole(rc);
            account.addRole(r);
        }
        AppUserDetails principal = new AppUserDetails(account);
        var auth = new UsernamePasswordAuthenticationToken(principal, "x", principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private static Role buildRole(String code) {
        try {
            // Role#Role() is protected and the entity has no field setters —
            // use reflection to produce an instance with the fields we need
            // for the AppUserDetails adapter to work (code + id).
            var ctor = Role.class.getDeclaredConstructor();
            ctor.setAccessible(true);
            Role r = ctor.newInstance();
            setField(r, "code", code);
            setField(r, "id", (long) code.hashCode() & 0xffffL);
            return r;
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    private static void setId(Object entity, long id) {
        setField(entity, "id", id);
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
