package com.att.tdp.issueflow.ticket.csv;

import static org.assertj.core.api.Assertions.assertThat;

import com.att.tdp.issueflow.support.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

class TicketCsvIntegrationTest extends AbstractIntegrationTest {

    @Test
    void csvImportReportsPartialSuccess() {
        String admin = loginAsAdmin();
        long projectId = createProject(admin);

        String csv = """
                title,description,status,priority,type,assigneeId
                Valid One,first,TODO,LOW,BUG,
                Valid Two,second,IN_PROGRESS,HIGH,FEATURE,
                Broken,bad enum,NOPE,LOW,BUG,
                """;

        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("file", new ByteArrayResource(csv.getBytes(StandardCharsets.UTF_8)) {
            @Override
            public String getFilename() {
                return "import.csv";
            }
        });
        form.add("projectId", projectId);

        HttpHeaders headers = bearer(admin);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        ResponseEntity<JsonNode> response = rest.postForEntity("/tickets/import",
                new HttpEntity<>(form, headers), JsonNode.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().get("created").asInt()).isEqualTo(2);
        assertThat(response.getBody().get("failed").asInt()).isEqualTo(1);
        assertThat(response.getBody().get("errors").size()).isEqualTo(1);
    }
}
