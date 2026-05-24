package com.att.tdp.issueflow.ticket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.att.tdp.issueflow.support.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

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
    void serializesIsOverdueWithExactContractKey() {
        // The README contract publishes "isOverdue". Jackson's boolean-getter
        // heuristic can silently emit "overdue" instead for a record component
        // named isOverdue — assert both the POST and GET responses use the
        // exact contract key.
        String admin = loginAsAdmin();
        long projectId = createProject(admin);

        ResponseEntity<JsonNode> created = rest.postForEntity("/tickets",
                new HttpEntity<>(Map.of("title", "IT Ticket " + uniqueSuffix(),
                        "status", "TODO", "priority", "LOW", "type", "BUG",
                        "projectId", projectId), jsonBearer(admin)), JsonNode.class);
        JsonNode createBody = created.getBody();
        assertThat(createBody.has("isOverdue")).isTrue();
        assertThat(createBody.has("overdue")).isFalse();

        long ticketId = createBody.get("id").asLong();
        ResponseEntity<JsonNode> fetched = rest.exchange("/tickets/" + ticketId,
                HttpMethod.GET, new HttpEntity<>(bearer(admin)), JsonNode.class);
        JsonNode getBody = fetched.getBody();
        assertThat(getBody.has("isOverdue")).isTrue();
        assertThat(getBody.has("overdue")).isFalse();
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
