package com.att.tdp.issueflow.scheduling;

import com.att.tdp.issueflow.common.audit.AuditPublisher;
import com.att.tdp.issueflow.common.enums.AuditAction;
import com.att.tdp.issueflow.common.enums.AuditActor;
import com.att.tdp.issueflow.common.enums.AuditEntityType;
import com.att.tdp.issueflow.common.enums.TicketPriority;
import com.att.tdp.issueflow.common.enums.TicketStatus;
import com.att.tdp.issueflow.ticket.Ticket;
import com.att.tdp.issueflow.ticket.TicketRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class EscalationScheduler {

    private final TicketRepository ticketRepository;
    private final AuditPublisher auditPublisher;

    public EscalationScheduler(TicketRepository ticketRepository,
                               AuditPublisher auditPublisher) {
        this.ticketRepository = ticketRepository;
        this.auditPublisher = auditPublisher;
    }

    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void escalateOverdueTickets() {
        List<Ticket> tickets = ticketRepository.findEscalatable(
                Instant.now(), TicketStatus.DONE, TicketPriority.CRITICAL);
        for (Ticket ticket : tickets) {
            escalate(ticket);
        }
    }

    private void escalate(Ticket ticket) {
        if (ticket.getPriority() != TicketPriority.CRITICAL) {
            // Promote one level: LOW -> MEDIUM -> HIGH -> CRITICAL.
            ticket.setPriority(nextPriority(ticket.getPriority()));
        } else {
            // Already CRITICAL and overdue -> set the terminal flag. Idempotent:
            // findEscalatable excludes CRITICAL tickets already flagged overdue.
            ticket.setOverdue(true);
        }
        auditPublisher.publish(AuditAction.AUTO_ESCALATE, AuditEntityType.TICKET,
                ticket.getId(), AuditActor.SYSTEM, null);
    }

    private TicketPriority nextPriority(TicketPriority current) {
        return TicketPriority.values()[current.ordinal() + 1];
    }
}
