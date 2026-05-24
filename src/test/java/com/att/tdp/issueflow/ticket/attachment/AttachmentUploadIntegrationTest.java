package com.att.tdp.issueflow.ticket.attachment;

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

/**
 * Exercises {@code AttachmentController.upload}: happy path with a valid PNG
 * payload, rejection of declared content types outside the whitelist, and
 * rejection of payloads whose magic bytes don't match the declared type
 * ({@link FileTypeValidator}). The 10MB cap is enforced by Spring's multipart
 * configuration (not the service), so it is not exercised here — covered by
 * the unit-level {@code FileTypeValidatorTest} + Spring's own multipart guard.
 */
class AttachmentUploadIntegrationTest extends AbstractIntegrationTest {

    private static final byte[] PNG_SIGNATURE =
            {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

    @Test
    void uploadsValidPngAndReturnsContractFields() {
        String admin = loginAsAdmin();
        long ticketId = createTicket(admin, createProject(admin));

        ResponseEntity<JsonNode> response = upload(admin, ticketId,
                "logo.png", MediaType.IMAGE_PNG_VALUE, paddedPng());

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        JsonNode body = response.getBody();
        assertThat(body.get("filename").asText()).isEqualTo("logo.png");
        assertThat(body.get("contentType").asText()).isEqualTo(MediaType.IMAGE_PNG_VALUE);
        assertThat(body.get("ticketId").asLong()).isEqualTo(ticketId);
        assertThat(body.get("id").asLong()).isPositive();
    }

    @Test
    void rejectsDisallowedContentType() {
        String admin = loginAsAdmin();
        long ticketId = createTicket(admin, createProject(admin));

        ResponseEntity<JsonNode> response = upload(admin, ticketId,
                "danger.zip", "application/zip", new byte[]{0x50, 0x4B, 0x03, 0x04});

        assertThat(response.getStatusCode().value()).isEqualTo(400);
    }

    @Test
    void rejectsMagicByteMismatch() {
        String admin = loginAsAdmin();
        long ticketId = createTicket(admin, createProject(admin));

        // Declare PNG but send arbitrary bytes — magic-byte check must reject.
        byte[] notAPng = "this is plain text not a png".getBytes(StandardCharsets.UTF_8);
        ResponseEntity<JsonNode> response = upload(admin, ticketId,
                "fake.png", MediaType.IMAGE_PNG_VALUE, notAPng);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
    }

    private ResponseEntity<JsonNode> upload(String token, long ticketId,
                                            String filename, String contentType, byte[] bytes) {
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        ByteArrayResource part = new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return filename;
            }
        };
        HttpHeaders partHeaders = new HttpHeaders();
        partHeaders.setContentType(MediaType.parseMediaType(contentType));
        form.add("file", new HttpEntity<>(part, partHeaders));

        HttpHeaders headers = bearer(token);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        return rest.postForEntity("/tickets/" + ticketId + "/attachments",
                new HttpEntity<>(form, headers), JsonNode.class);
    }

    private byte[] paddedPng() {
        // Just the 8-byte PNG signature; FileTypeValidator only inspects the leading
        // signature bytes, so a minimal stub is sufficient.
        byte[] bytes = new byte[PNG_SIGNATURE.length];
        System.arraycopy(PNG_SIGNATURE, 0, bytes, 0, PNG_SIGNATURE.length);
        return bytes;
    }
}
