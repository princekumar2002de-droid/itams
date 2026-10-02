package com.princekumar.itams.ticket;

import com.princekumar.itams.asset.Asset;
import com.princekumar.itams.asset.AssetRepository;
import com.princekumar.itams.common.exception.BusinessRuleViolationException;
import com.princekumar.itams.common.exception.ResourceNotFoundException;
import com.princekumar.itams.person.Person;
import com.princekumar.itams.person.PersonRepository;
import com.princekumar.itams.ticket.dto.CreateTicketRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link TicketService} — the wiring around the entity state
 * machine (tested separately in {@link TicketStateMachineTest}).
 */
@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock TicketRepository repo;
    @Mock TicketCommentRepository commentRepo;
    @Mock PersonRepository personRepo;
    @Mock AssetRepository assetRepo;
    @InjectMocks TicketService service;

    @Test
    void create_persists_and_generates_ticket_number() {
        Person p = new Person("Alex", "K", "alex@example.com", null);
        var req = new CreateTicketRequest("Laptop overheating", "Fans loud",
            TicketPriority.MEDIUM, 7L, null);
        when(personRepo.findByIdAndDeletedAtIsNull(7L)).thenReturn(Optional.of(p));
        // Simulate JPA giving us an id + created timestamp on save.
        when(repo.save(any(Ticket.class))).thenAnswer(inv -> {
            Ticket t = inv.getArgument(0);
            setId(t, 123L);
            return t;
        });

        Ticket created = service.create(req);

        assertThat(created.getSubject()).isEqualTo("Laptop overheating");
        assertThat(created.getStatus()).isEqualTo(TicketStatus.OPEN);
        assertThat(created.getReporter()).isSameAs(p);
        assertThat(created.getRelatedAsset()).isNull();
        // Final ticket number follows the TCK-YYYY-NNNNNN pattern (year defaulted
        // to 2026 by the service when createdAt is null in tests).
        assertThat(created.getTicketNumber()).matches("TCK-\\d{4}-000123");
    }

    @Test
    void create_wires_related_asset_when_given() {
        Person p = new Person("Alex", "K", "alex@example.com", null);
        Asset asset = new Asset("A-1", null, "SN", LocalDate.now(), new BigDecimal("100"), null, null);
        var req = new CreateTicketRequest("Screen dead", "Nothing on boot",
            TicketPriority.HIGH, 7L, 42L);
        when(personRepo.findByIdAndDeletedAtIsNull(7L)).thenReturn(Optional.of(p));
        when(assetRepo.findById(42L)).thenReturn(Optional.of(asset));
        when(repo.save(any(Ticket.class))).thenAnswer(inv -> {
            Ticket t = inv.getArgument(0);
            setId(t, 5L);
            return t;
        });

        Ticket created = service.create(req);
        assertThat(created.getRelatedAsset()).isSameAs(asset);
        assertThat(created.getPriority()).isEqualTo(TicketPriority.HIGH);
    }

    @Test
    void create_rejects_missing_reporter() {
        var req = new CreateTicketRequest("X", "Y", TicketPriority.LOW, 999L, null);
        when(personRepo.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(req))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("999");
    }

    @Test
    void transitionStatus_wraps_illegal_state_as_business_rule_violation() {
        Ticket t = new Ticket("s", "d", TicketPriority.LOW, null, null);
        setId(t, 1L);
        when(repo.findById(1L)).thenReturn(Optional.of(t));

        // OPEN → RESOLVED is illegal per the state machine.
        assertThatThrownBy(() -> service.transitionStatus(1L, TicketStatus.RESOLVED))
            .isInstanceOf(BusinessRuleViolationException.class)
            .hasMessageContaining("Illegal ticket transition");
    }

    @Test
    void transitionStatus_advances_valid_transition() {
        Ticket t = new Ticket("s", "d", TicketPriority.LOW, null, null);
        setId(t, 1L);
        when(repo.findById(1L)).thenReturn(Optional.of(t));

        Ticket updated = service.transitionStatus(1L, TicketStatus.IN_PROGRESS);
        assertThat(updated.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
    }

    // ── reflection helper to set BaseEntity#id without a public setter ──────

    private static void setId(Object entity, long id) {
        try {
            var f = entity.getClass().getSuperclass().getDeclaredField("id");
            f.setAccessible(true);
            f.set(entity, id);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }
}
