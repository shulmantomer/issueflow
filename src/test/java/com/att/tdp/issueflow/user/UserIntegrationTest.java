package com.att.tdp.issueflow.user;

import static org.assertj.core.api.Assertions.assertThat;

import com.att.tdp.issueflow.support.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

class UserIntegrationTest extends AbstractIntegrationTest {

    @Test
    void createUserWithoutPasswordReturnsGeneratedPassword() {
        String username = "ituser_" + uniqueSuffix();
        ResponseEntity<JsonNode> response = rest.postForEntity("/users",
                Map.of("username", username, "email", username + "@test.com",
                        "fullName", "IT User", "role", "DEVELOPER"), JsonNode.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().get("generatedPassword").asText()).isNotBlank();
    }

    @Test
    void rejectsDuplicateUsernameCaseInsensitively() {
        // 'admin1' is seeded; 'ADMIN1' must collide.
        ResponseEntity<String> response = rest.postForEntity("/users",
                Map.of("username", "ADMIN1", "email", "x" + uniqueSuffix() + "@test.com",
                        "fullName", "X", "role", "ADMIN"), String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(409);
    }

    @Test
    void cannotDeleteUserOwningActiveProject() {
        String admin = loginAsAdmin();
        // admin1 (id 1) owns seeded project 'Phoenix'.
        ResponseEntity<String> response = rest.exchange("/users/1", HttpMethod.DELETE,
                new HttpEntity<>(bearer(admin)), String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(409);
    }
}
