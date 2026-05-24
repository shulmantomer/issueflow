package com.att.tdp.issueflow.ticket;

import static org.assertj.core.api.Assertions.assertThat;

import com.att.tdp.issueflow.support.AbstractIntegrationTest;
import com.att.tdp.issueflow.user.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

/**
 * Exercises {@code TicketService.autoAssignLeastLoadedDeveloper}: the rule is
 * "least-loaded DEVELOPER, tie-break by user id ASC". Seeded developers are
 * users 3, 4, 5, 6 — in a fresh project all four have 0 open tickets, so the
 * first auto-assigned ticket must go to user 3, the second to user 4, and so on.
 */
class AutoAssignmentIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void picksLowestUserIdWhenWorkloadsAreTied() {
        String admin = loginAsAdmin();
        long projectId = createProject(admin);

        long firstTicket = createTicket(admin, projectId);
        long firstAssignee = ticketAssignee(admin, firstTicket);
        assertThat(firstAssignee).isEqualTo(3L); // developer1, smallest id

        long secondTicket = createTicket(admin, projectId);
        long secondAssignee = ticketAssignee(admin, secondTicket);
        // developer1 now has one open ticket — next assignee is the next-smallest id.
        assertThat(secondAssignee).isEqualTo(4L);
    }

    @Test
    void emitsSystemAutoAssignAuditRow() {
        String admin = loginAsAdmin();
        long projectId = createProject(admin);
        long ticketId = createTicket(admin, projectId);

        JsonNode systemRows = fetch(admin,
                "/audit-logs?entityType=TICKET&entityId=" + ticketId + "&action=AUTO_ASSIGN");
        assertThat(systemRows.size()).isEqualTo(1);
        assertThat(systemRows.get(0).get("actor").asText()).isEqualTo("SYSTEM");
    }

    @Test
    void workloadQueryReturnsZeroForEveryDeveloperOnFreshProject() {
        // Direct repository assertion: the SQL query that drives auto-assignment
        // must yield one row per DEVELOPER, ordered by count ASC then id ASC.
        String admin = loginAsAdmin();
        long projectId = createProject(admin);

        List<Object[]> workload = userRepository.findDeveloperWorkload(projectId);
        assertThat(workload).hasSize(4);
        long prevId = Long.MIN_VALUE;
        long prevCount = -1;
        for (Object[] row : workload) {
            long userId = ((Number) row[0]).longValue();
            long count = ((Number) row[2]).longValue();
            assertThat(count).isZero();
            assertThat(count).isGreaterThanOrEqualTo(prevCount);
            if (count == prevCount) {
                assertThat(userId).isGreaterThan(prevId);
            }
            prevCount = count;
            prevId = userId;
        }
    }

    private long ticketAssignee(String token, long ticketId) {
        ResponseEntity<JsonNode> response = rest.exchange("/tickets/" + ticketId,
                HttpMethod.GET, new HttpEntity<>(bearer(token)), JsonNode.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        return response.getBody().get("assigneeId").asLong();
    }

    private JsonNode fetch(String token, String path) {
        ResponseEntity<JsonNode> response = rest.exchange(path, HttpMethod.GET,
                new HttpEntity<>(bearer(token)), JsonNode.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        return response.getBody();
    }
}
