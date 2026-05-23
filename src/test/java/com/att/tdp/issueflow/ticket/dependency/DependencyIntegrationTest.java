package com.att.tdp.issueflow.ticket.dependency;

import static org.assertj.core.api.Assertions.assertThat;

import com.att.tdp.issueflow.support.AbstractIntegrationTest;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;

class DependencyIntegrationTest extends AbstractIntegrationTest {

    @Test
    void ticketCannotReachDoneWhileBlockedByUnresolvedTicket() {
        String admin = loginAsAdmin();
        long projectId = createProject(admin);
        long blocker = createTicket(admin, projectId);
        long blocked = createTicket(admin, projectId);

        rest.postForEntity("/tickets/" + blocked + "/dependencies",
                new HttpEntity<>(Map.of("blockedBy", blocker), jsonBearer(admin)), Void.class);

        patchTicket(admin, blocked, Map.of("status", "IN_PROGRESS"));
        patchTicket(admin, blocked, Map.of("status", "IN_REVIEW"));
        assertThat(patchTicket(admin, blocked, Map.of("status", "DONE"))
                .getStatusCode().value()).isEqualTo(409);

        patchTicket(admin, blocker, Map.of("status", "IN_PROGRESS"));
        patchTicket(admin, blocker, Map.of("status", "IN_REVIEW"));
        patchTicket(admin, blocker, Map.of("status", "DONE"));

        assertThat(patchTicket(admin, blocked, Map.of("status", "DONE"))
                .getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void rejectsCircularDependency() {
        String admin = loginAsAdmin();
        long projectId = createProject(admin);
        long ticketA = createTicket(admin, projectId);
        long ticketB = createTicket(admin, projectId);

        rest.postForEntity("/tickets/" + ticketA + "/dependencies",
                new HttpEntity<>(Map.of("blockedBy", ticketB), jsonBearer(admin)), Void.class);

        ResponseEntity<String> cyclic = rest.postForEntity(
                "/tickets/" + ticketB + "/dependencies",
                new HttpEntity<>(Map.of("blockedBy", ticketA), jsonBearer(admin)), String.class);
        assertThat(cyclic.getStatusCode().value()).isEqualTo(409);
    }
}
