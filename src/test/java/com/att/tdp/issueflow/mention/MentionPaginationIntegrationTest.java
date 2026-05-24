package com.att.tdp.issueflow.mention;

import static org.assertj.core.api.Assertions.assertThat;

import com.att.tdp.issueflow.support.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

/**
 * Verifies {@code GET /users/:userId/mentions}: page math, total count, and
 * input validation. Seeds three comments mentioning developer3 (user id 5),
 * then walks the pages.
 */
class MentionPaginationIntegrationTest extends AbstractIntegrationTest {

    @Test
    void paginatesMentionsByPageAndPageSize() {
        String admin = loginAsAdmin();
        long ticketId = createTicket(admin, createProject(admin));
        seedComment(admin, ticketId, "first @developer3");
        seedComment(admin, ticketId, "second @developer3");
        seedComment(admin, ticketId, "third @developer3");

        JsonNode firstPage = fetch(admin, "/users/5/mentions?page=1&pageSize=2");
        assertThat(firstPage.get("page").asInt()).isEqualTo(1);
        assertThat(firstPage.get("total").asLong()).isGreaterThanOrEqualTo(3);
        assertThat(firstPage.get("data").size()).isEqualTo(2);

        JsonNode secondPage = fetch(admin, "/users/5/mentions?page=2&pageSize=2");
        assertThat(secondPage.get("page").asInt()).isEqualTo(2);
        assertThat(secondPage.get("data").size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void rejectsInvalidPaginationParameters() {
        String admin = loginAsAdmin();

        ResponseEntity<JsonNode> badPage = rest.exchange("/users/5/mentions?page=0",
                HttpMethod.GET, new HttpEntity<>(bearer(admin)), JsonNode.class);
        assertThat(badPage.getStatusCode().value()).isEqualTo(400);

        ResponseEntity<JsonNode> badSize = rest.exchange("/users/5/mentions?pageSize=0",
                HttpMethod.GET, new HttpEntity<>(bearer(admin)), JsonNode.class);
        assertThat(badSize.getStatusCode().value()).isEqualTo(400);
    }

    @Test
    void returns404ForUnknownUser() {
        String admin = loginAsAdmin();
        ResponseEntity<JsonNode> response = rest.exchange("/users/999999/mentions",
                HttpMethod.GET, new HttpEntity<>(bearer(admin)), JsonNode.class);
        assertThat(response.getStatusCode().value()).isEqualTo(404);
    }

    private void seedComment(String token, long ticketId, String content) {
        ResponseEntity<JsonNode> response = rest.postForEntity(
                "/tickets/" + ticketId + "/comments",
                new HttpEntity<>(Map.of("authorId", 1, "content", content), jsonBearer(token)),
                JsonNode.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }

    private JsonNode fetch(String token, String path) {
        ResponseEntity<JsonNode> response = rest.exchange(path, HttpMethod.GET,
                new HttpEntity<>(bearer(token)), JsonNode.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        return response.getBody();
    }
}
