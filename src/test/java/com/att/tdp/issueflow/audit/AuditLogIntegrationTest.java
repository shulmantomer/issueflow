package com.att.tdp.issueflow.audit;

import static org.assertj.core.api.Assertions.assertThat;

import com.att.tdp.issueflow.support.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

class AuditLogIntegrationTest extends AbstractIntegrationTest {

    @Test
    void filtersByEntityTypeAndAction() {
        String admin = loginAsAdmin();
        long projectId = createProject(admin);
        long ticketId = createTicket(admin, projectId);

        JsonNode ticketCreates = fetch(admin,
                "/audit-logs?entityType=TICKET&action=CREATE");
        assertThat(ticketCreates.isArray()).isTrue();
        assertThat(containsAuditFor(ticketCreates, "TICKET", "CREATE", ticketId)).isTrue();
        // The PROJECT create row from the same setup must be filtered out.
        assertThat(everyRowMatches(ticketCreates, "TICKET", "CREATE")).isTrue();
    }

    @Test
    void filtersBySystemActorForAutoAssign() {
        String admin = loginAsAdmin();
        long projectId = createProject(admin);
        long ticketId = createTicket(admin, projectId);

        // The unassigned ticket triggers AUTO_ASSIGN with actor=SYSTEM.
        JsonNode systemRows = fetch(admin, "/audit-logs?actor=SYSTEM");
        assertThat(containsAuditFor(systemRows, "TICKET", "AUTO_ASSIGN", ticketId)).isTrue();
        for (JsonNode row : systemRows) {
            assertThat(row.get("actor").asText()).isEqualTo("SYSTEM");
        }
    }

    @Test
    void scopesByEntityId() {
        String admin = loginAsAdmin();
        long projectId = createProject(admin);
        long ticketA = createTicket(admin, projectId);
        long ticketB = createTicket(admin, projectId);

        JsonNode onlyTicketA = fetch(admin,
                "/audit-logs?entityType=TICKET&entityId=" + ticketA);
        for (JsonNode row : onlyTicketA) {
            assertThat(row.get("entityType").asText()).isEqualTo("TICKET");
            assertThat(row.get("entityId").asLong()).isEqualTo(ticketA);
        }
        assertThat(containsAuditFor(onlyTicketA, "TICKET", "CREATE", ticketA)).isTrue();
        assertThat(containsAuditFor(onlyTicketA, "TICKET", "CREATE", ticketB)).isFalse();
    }

    private JsonNode fetch(String token, String path) {
        ResponseEntity<JsonNode> response = rest.exchange(path, HttpMethod.GET,
                new HttpEntity<>(bearer(token)), JsonNode.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        return response.getBody();
    }

    private boolean containsAuditFor(JsonNode rows, String entityType, String action, long entityId) {
        for (JsonNode row : rows) {
            if (entityType.equals(row.get("entityType").asText())
                    && action.equals(row.get("action").asText())
                    && row.get("entityId").asLong() == entityId) {
                return true;
            }
        }
        return false;
    }

    private boolean everyRowMatches(JsonNode rows, String entityType, String action) {
        for (JsonNode row : rows) {
            if (!entityType.equals(row.get("entityType").asText())
                    || !action.equals(row.get("action").asText())) {
                return false;
            }
        }
        return true;
    }
}
