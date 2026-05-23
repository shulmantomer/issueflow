package com.att.tdp.issueflow.ticket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.att.tdp.issueflow.support.AbstractIntegrationTest;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;

class TicketIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TicketRepository ticketRepository;

    @Test
    void enforcesStrictOneStepForwardTransition() {
        String admin = loginAsAdmin();
        long ticketId = createTicket(admin, createProject(admin));

        assertThat(patchTicket(admin, ticketId, Map.of("status", "IN_REVIEW"))
                .getStatusCode().value()).isEqualTo(409);
        assertThat(patchTicket(admin, ticketId, Map.of("status", "IN_PROGRESS"))
                .getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void cannotUpdateTicketOnceDone() {
        String admin = loginAsAdmin();
        long ticketId = createTicket(admin, createProject(admin));
        patchTicket(admin, ticketId, Map.of("status", "IN_PROGRESS"));
        patchTicket(admin, ticketId, Map.of("status", "IN_REVIEW"));
        patchTicket(admin, ticketId, Map.of("status", "DONE"));

        assertThat(patchTicket(admin, ticketId, Map.of("title", "after done"))
                .getStatusCode().value()).isEqualTo(409);
    }

    @Test
    void rejectsStaleUpdateViaOptimisticLocking() {
        String admin = loginAsAdmin();
        long ticketId = createTicket(admin, createProject(admin));

        // Capture the entity at its current version.
        Ticket stale = ticketRepository.findById(ticketId).orElseThrow();
        // A real HTTP update bumps the version in the database.
        patchTicket(admin, ticketId, Map.of("title", "fresh title"));

        // Persisting the now-stale entity must trip the @Version check.
        stale.setTitle("stale write");
        assertThatThrownBy(() -> ticketRepository.saveAndFlush(stale))
                .isInstanceOf(OptimisticLockingFailureException.class);
    }
}
