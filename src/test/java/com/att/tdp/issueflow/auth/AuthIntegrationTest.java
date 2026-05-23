package com.att.tdp.issueflow.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.att.tdp.issueflow.support.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

class AuthIntegrationTest extends AbstractIntegrationTest {

    @Test
    void loginThenMeReturnsProfile() {
        String token = login("admin1", "password123");
        assertThat(token).isNotBlank();

        ResponseEntity<JsonNode> me = rest.exchange("/auth/me", HttpMethod.GET,
                new HttpEntity<>(bearer(token)), JsonNode.class);

        assertThat(me.getStatusCode().value()).isEqualTo(200);
        assertThat(me.getBody().get("username").asText()).isEqualTo("admin1");
    }

    @Test
    void rejectsRequestWithoutToken() {
        assertThat(rest.getForEntity("/users", String.class).getStatusCode().value())
                .isEqualTo(401);
    }

    @Test
    void rejectsBadCredentials() {
        ResponseEntity<String> response = rest.postForEntity("/auth/login",
                Map.of("username", "admin1", "password", "wrong-password"), String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void logoutDenylistsTheToken() {
        String token = login("developer1", "password123");
        HttpEntity<Void> request = new HttpEntity<>(bearer(token));

        assertThat(rest.exchange("/auth/me", HttpMethod.GET, request, String.class)
                .getStatusCode().value()).isEqualTo(200);

        rest.exchange("/auth/logout", HttpMethod.POST, request, Void.class);

        assertThat(rest.exchange("/auth/me", HttpMethod.GET, request, String.class)
                .getStatusCode().value()).isEqualTo(401);
    }
}
