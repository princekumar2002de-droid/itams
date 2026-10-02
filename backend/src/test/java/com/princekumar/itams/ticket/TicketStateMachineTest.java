package com.princekumar.itams.ticket;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pure entity tests for the {@link Ticket} state machine. No Spring context,
 * no mocks — the transitions are enforced on the entity itself, so we test
 * them at that level and let the service tests handle wiring.
 */
class TicketStateMachineTest {

    @Test
    void open_can_move_to_in_progress_or_closed() {
        assertThat(newTicket().getStatus()).isEqualTo(TicketStatus.OPEN);

        Ticket a = newTicket();
        a.transitionTo(TicketStatus.IN_PROGRESS);
        assertThat(a.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);

        Ticket b = newTicket();
        b.transitionTo(TicketStatus.CLOSED);
        assertThat(b.getStatus()).isEqualTo(TicketStatus.CLOSED);
        assertThat(b.getClosedAt()).isNotNull();
    }

    @Test
    void open_cannot_jump_straight_to_resolved() {
        Ticket t = newTicket();
        assertThatThrownBy(() -> t.transitionTo(TicketStatus.RESOLVED))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("OPEN")
            .hasMessageContaining("RESOLVED");
    }

    @Test
    void in_progress_can_wait_or_resolve() {
        Ticket t = newTicket();
        t.transitionTo(TicketStatus.IN_PROGRESS);
        t.transitionTo(TicketStatus.RESOLVED);
        assertThat(t.getStatus()).isEqualTo(TicketStatus.RESOLVED);
        assertThat(t.getResolvedAt()).isNotNull();
    }

    @Test
    void resolved_can_reopen_to_in_progress() {
        Ticket t = newTicket();
        t.transitionTo(TicketStatus.IN_PROGRESS);
        t.transitionTo(TicketStatus.RESOLVED);
        t.transitionTo(TicketStatus.IN_PROGRESS);
        assertThat(t.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
    }

    @Test
    void closed_is_terminal() {
        Ticket t = newTicket();
        t.transitionTo(TicketStatus.CLOSED);
        for (TicketStatus s : TicketStatus.values()) {
            assertThatThrownBy(() -> t.transitionTo(s))
                .as("closed → %s must be illegal", s)
                .isInstanceOf(IllegalStateException.class);
        }
    }

    @Test
    void waiting_cannot_be_reached_from_open() {
        Ticket t = newTicket();
        assertThatThrownBy(() -> t.transitionTo(TicketStatus.WAITING))
            .isInstanceOf(IllegalStateException.class);
    }

    private static Ticket newTicket() {
        // reporter + relatedAsset are only stored, never inspected in transitions
        return new Ticket("Broken screen", "Screen flickers", TicketPriority.MEDIUM, null, null);
    }
}
