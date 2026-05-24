package com.att.tdp.issueflow.scheduling;

import static org.assertj.core.api.Assertions.assertThat;

import com.att.tdp.issueflow.common.enums.TicketPriority;
import com.att.tdp.issueflow.common.enums.TicketStatus;
import com.att.tdp.issueflow.support.AbstractIntegrationTest;
import com.att.tdp.issueflow.ticket.Ticket;
import com.att.tdp.issueflow.ticket.TicketRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Exercises {@link EscalationScheduler#escalateOverdueTickets()} directly,
 * bypassing the cron trigger so the test does not depend on wall-clock time.
 * Covers the four cases of the escalation rule: promote one level for non-
 * CRITICAL overdue tickets (LOW→MEDIUM, MEDIUM→HIGH, HIGH→CRITICAL), flip
 * {@code is_overdue=true} for already-CRITICAL overdue tickets, idempotency
 * for fully-escalated tickets, and exemption of DONE / future-dated tickets.
 */
class EscalationSchedulerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private EscalationScheduler scheduler;

    @Autowired
    private TicketRepository ticketRepository;

    @Test
    void promotesOverduePriorityOneStepForLowMediumHigh() {
        long lowId = seedOverdue(TicketStatus.TODO, TicketPriority.LOW);
        long mediumId = seedOverdue(TicketStatus.IN_PROGRESS, TicketPriority.MEDIUM);
        long highId = seedOverdue(TicketStatus.IN_REVIEW, TicketPriority.HIGH);

        scheduler.escalateOverdueTickets();

        assertThat(ticketRepository.findById(lowId).orElseThrow().getPriority())
                .isEqualTo(TicketPriority.MEDIUM);
        assertThat(ticketRepository.findById(mediumId).orElseThrow().getPriority())
                .isEqualTo(TicketPriority.HIGH);
        assertThat(ticketRepository.findById(highId).orElseThrow().getPriority())
                .isEqualTo(TicketPriority.CRITICAL);
    }

    @Test
    void flagsCriticalOverdueAsOverdueAndIsIdempotent() {
        long criticalId = seedOverdue(TicketStatus.TODO, TicketPriority.CRITICAL);

        scheduler.escalateOverdueTickets();
        Ticket flipped = ticketRepository.findById(criticalId).orElseThrow();
        assertThat(flipped.getPriority()).isEqualTo(TicketPriority.CRITICAL);
        assertThat(flipped.isOverdue()).isTrue();

        // Second pass must be a no-op — findEscalatable excludes already-flagged rows.
        scheduler.escalateOverdueTickets();
        Ticket again = ticketRepository.findById(criticalId).orElseThrow();
        assertThat(again.isOverdue()).isTrue();
        assertThat(again.getPriority()).isEqualTo(TicketPriority.CRITICAL);
    }

    @Test
    void skipsDoneTicketsAndFutureDueDates() {
        long doneId = seedTicket(TicketStatus.DONE, TicketPriority.LOW,
                Instant.now().minus(1, ChronoUnit.DAYS));
        long futureId = seedTicket(TicketStatus.TODO, TicketPriority.LOW,
                Instant.now().plus(1, ChronoUnit.DAYS));

        scheduler.escalateOverdueTickets();

        assertThat(ticketRepository.findById(doneId).orElseThrow().getPriority())
                .isEqualTo(TicketPriority.LOW);
        assertThat(ticketRepository.findById(futureId).orElseThrow().getPriority())
                .isEqualTo(TicketPriority.LOW);
    }

    private long seedOverdue(TicketStatus status, TicketPriority priority) {
        return seedTicket(status, priority, Instant.now().minus(7, ChronoUnit.DAYS));
    }

    private long seedTicket(TicketStatus status, TicketPriority priority, Instant dueDate) {
        // Re-use the seeded admin/project rather than minting fresh ones — we
        // only need a valid project_id; tickets are inserted directly.
        Ticket ticket = Ticket.builder()
                .title("escalation-test-" + uniqueSuffix())
                .description("seeded for scheduler test")
                .status(status)
                .priority(priority)
                .type(com.att.tdp.issueflow.common.enums.TicketType.BUG)
                .projectId(1L)
                .dueDate(dueDate)
                .overdue(false)
                .build();
        return ticketRepository.save(ticket).getId();
    }
}
