package com.att.tdp.issueflow.support;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;

/**
 * Base class for integration tests. Boots the full Spring context against the
 * PostgreSQL instance from compose.yml (localhost:5432, per application.yaml).
 * Requires `docker compose up -d` before running the test suite.
 *
 * <p>An earlier iteration of this base class used Testcontainers to provision a
 * hermetic Postgres per JVM. That setup was reverted because Testcontainers'
 * Windows/JNA path cannot reach Docker Desktop's {@code dockerDesktopLinuxEngine}
 * pipe on this environment, and the {@code docker_engine} fallback returns a
 * 400 redirect. Using the compose-managed Postgres is also the path the
 * assignment prescribes (req 4.2).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractIntegrationTest {

    private static final AtomicLong UNIQUE = new AtomicLong();

    @Autowired
    protected TestRestTemplate rest;

    @BeforeEach
    void enablePatchSupport() {
        // The default request factory cannot issue PATCH; the JDK HttpClient one can.
        rest.getRestTemplate().setRequestFactory(new JdkClientHttpRequestFactory());
    }

    protected String uniqueSuffix() {
        return System.nanoTime() + "_" + UNIQUE.incrementAndGet();
    }

    protected String login(String username, String password) {
        ResponseEntity<JsonNode> response = rest.postForEntity("/auth/login",
                Map.of("username", username, "password", password), JsonNode.class);
        return response.getBody().get("accessToken").asText();
    }

    protected String loginAsAdmin() {
        return login("admin1", "password123");
    }

    protected HttpHeaders bearer(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    protected HttpHeaders jsonBearer(String token) {
        HttpHeaders headers = bearer(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    protected long createProject(String token) {
        ResponseEntity<JsonNode> response = rest.postForEntity("/projects",
                new HttpEntity<>(Map.of("name", "IT Project " + uniqueSuffix(),
                        "ownerId", 1), jsonBearer(token)), JsonNode.class);
        return response.getBody().get("id").asLong();
    }

    protected long createTicket(String token, long projectId) {
        ResponseEntity<JsonNode> response = rest.postForEntity("/tickets",
                new HttpEntity<>(Map.of("title", "IT Ticket " + uniqueSuffix(),
                        "status", "TODO", "priority", "LOW", "type", "BUG",
                        "projectId", projectId), jsonBearer(token)), JsonNode.class);
        return response.getBody().get("id").asLong();
    }

    protected ResponseEntity<JsonNode> patchTicket(String token, long ticketId,
                                                   Map<String, Object> body) {
        return rest.exchange("/tickets/" + ticketId, HttpMethod.PATCH,
                new HttpEntity<>(body, jsonBearer(token)), JsonNode.class);
    }
}
