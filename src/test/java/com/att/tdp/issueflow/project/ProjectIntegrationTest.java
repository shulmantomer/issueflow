package com.att.tdp.issueflow.project;

import static org.assertj.core.api.Assertions.assertThat;

import com.att.tdp.issueflow.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;

class ProjectIntegrationTest extends AbstractIntegrationTest {

    @Test
    void projectSoftDeleteAndRestoreLifecycle() {
        String admin = loginAsAdmin();
        long projectId = createProject(admin);
        HttpEntity<Void> auth = new HttpEntity<>(bearer(admin));

        assertThat(rest.exchange("/projects/" + projectId, HttpMethod.DELETE, auth, Void.class)
                .getStatusCode().value()).isEqualTo(200);
        assertThat(rest.exchange("/projects/" + projectId, HttpMethod.GET, auth, String.class)
                .getStatusCode().value()).isEqualTo(404);
        assertThat(rest.exchange("/projects/" + projectId + "/restore", HttpMethod.POST, auth,
                Void.class).getStatusCode().value()).isEqualTo(200);
        assertThat(rest.exchange("/projects/" + projectId, HttpMethod.GET, auth, String.class)
                .getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void nonAdminCannotListDeletedProjects() {
        String developer = login("developer1", "password123");
        assertThat(rest.exchange("/projects/deleted", HttpMethod.GET,
                new HttpEntity<>(bearer(developer)), String.class)
                .getStatusCode().value()).isEqualTo(403);
    }
}
