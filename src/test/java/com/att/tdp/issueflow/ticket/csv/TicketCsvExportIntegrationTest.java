package com.att.tdp.issueflow.ticket.csv;

import static org.assertj.core.api.Assertions.assertThat;

import com.att.tdp.issueflow.support.AbstractIntegrationTest;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Exercises {@code TicketCsvController.export}: header layout, response
 * headers, and faithful round-trip of values that need CSV quoting (embedded
 * commas, double quotes, newlines).
 */
class TicketCsvExportIntegrationTest extends AbstractIntegrationTest {

    @Test
    void exportPreservesCommasAndQuotes() throws Exception {
        String admin = loginAsAdmin();
        long projectId = createProject(admin);

        // Plain string (not a text block) so the embedded "" sequences used by
        // CSV's quote-escaping convention don't get confused with the text-block
        // delimiter """.
        String csv = "title,description,status,priority,type,assigneeId\n"
                + "\"Comma, in title\",\"line1, line2\",TODO,LOW,BUG,\n"
                + "\"Quoted \"\"word\"\"\",\"contains \"\"quotes\"\"\",TODO,LOW,BUG,\n";
        importCsv(admin, projectId, csv);

        ResponseEntity<byte[]> response = rest.exchange(
                "/tickets/export?projectId=" + projectId,
                HttpMethod.GET, new HttpEntity<>(bearer(admin)), byte[].class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .contains("filename=\"tickets-project-" + projectId + ".csv\"");
        assertThat(response.getHeaders().getContentType().toString())
                .startsWith("text/csv");

        String body = new String(response.getBody(), StandardCharsets.UTF_8);
        try (CSVParser parser = CSVParser.parse(new StringReader(body),
                CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build())) {
            List<CSVRecord> rows = parser.getRecords();
            assertThat(rows).hasSizeGreaterThanOrEqualTo(2);
            assertThat(rows).anySatisfy(r -> {
                assertThat(r.get("title")).isEqualTo("Comma, in title");
                assertThat(r.get("description")).isEqualTo("line1, line2");
            });
            assertThat(rows).anySatisfy(r -> {
                assertThat(r.get("title")).isEqualTo("Quoted \"word\"");
                assertThat(r.get("description")).isEqualTo("contains \"quotes\"");
            });
        }
    }

    @Test
    void exportedCsvCanBeReImportedSuccessfully() {
        String admin = loginAsAdmin();
        long projectId = createProject(admin);

        importCsv(admin, projectId, """
                title,description,status,priority,type,assigneeId
                Roundtrip A,desc A,TODO,LOW,BUG,
                Roundtrip B,desc B,IN_PROGRESS,MEDIUM,FEATURE,
                """);

        ResponseEntity<byte[]> exported = rest.exchange(
                "/tickets/export?projectId=" + projectId,
                HttpMethod.GET, new HttpEntity<>(bearer(admin)), byte[].class);
        assertThat(exported.getStatusCode().value()).isEqualTo(200);

        long secondProject = createProject(admin);
        var result = importCsv(admin, secondProject,
                new String(exported.getBody(), StandardCharsets.UTF_8));
        assertThat(result.get("created").asInt()).isEqualTo(2);
        assertThat(result.get("failed").asInt()).isEqualTo(0);
    }

    private com.fasterxml.jackson.databind.JsonNode importCsv(
            String token, long projectId, String csv) {
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("file", new ByteArrayResource(csv.getBytes(StandardCharsets.UTF_8)) {
            @Override
            public String getFilename() {
                return "import.csv";
            }
        });
        form.add("projectId", projectId);

        HttpHeaders headers = bearer(token);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        ResponseEntity<com.fasterxml.jackson.databind.JsonNode> response = rest.postForEntity(
                "/tickets/import",
                new HttpEntity<>(form, headers),
                com.fasterxml.jackson.databind.JsonNode.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        return response.getBody();
    }
}
