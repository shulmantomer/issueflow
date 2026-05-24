package com.att.tdp.issueflow.comment;

import static org.assertj.core.api.Assertions.assertThat;

import com.att.tdp.issueflow.support.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

/**
 * Verifies the @-mention re-evaluation rule from
 * {@code CommentService.create/update}: on every write the {@code comment_mentions}
 * join table is recomputed from the body — added mentions are created, removed
 * mentions are deleted, and unknown @usernames are silently dropped.
 */
class CommentMentionIntegrationTest extends AbstractIntegrationTest {

    @Test
    void persistsMentionsOnCreate() {
        String admin = loginAsAdmin();
        long ticketId = createTicket(admin, createProject(admin));

        JsonNode created = postComment(admin, ticketId,
                "ping @developer1 and @developer2 here");

        assertThat(mentionedUserIds(created)).containsExactlyInAnyOrder(3L, 4L);
    }

    @Test
    void replacesMentionsOnUpdate() {
        String admin = loginAsAdmin();
        long ticketId = createTicket(admin, createProject(admin));

        JsonNode created = postComment(admin, ticketId, "see @developer1 @developer2");
        long commentId = created.get("id").asLong();
        assertThat(mentionedUserIds(created)).containsExactlyInAnyOrder(3L, 4L);

        ResponseEntity<Void> patch = rest.exchange(
                "/tickets/" + ticketId + "/comments/" + commentId,
                HttpMethod.PATCH,
                new HttpEntity<>(Map.of("content", "now @developer1 @developer3 only"),
                        jsonBearer(admin)),
                Void.class);
        assertThat(patch.getStatusCode().value()).isEqualTo(200);

        // Re-fetch the comment list and locate the updated row.
        JsonNode all = rest.exchange("/tickets/" + ticketId + "/comments",
                HttpMethod.GET, new HttpEntity<>(bearer(admin)), JsonNode.class).getBody();
        JsonNode updated = null;
        for (JsonNode c : all) {
            if (c.get("id").asLong() == commentId) {
                updated = c;
                break;
            }
        }
        assertThat(updated).isNotNull();
        // developer2 (id=4) removed, developer3 (id=5) added.
        assertThat(mentionedUserIds(updated)).containsExactlyInAnyOrder(3L, 5L);
    }

    @Test
    void silentlyDropsUnknownMentions() {
        String admin = loginAsAdmin();
        long ticketId = createTicket(admin, createProject(admin));

        JsonNode created = postComment(admin, ticketId,
                "ghost @who_is_this and real @developer1");

        // Only the real user resolves; unknown mention is not an error.
        assertThat(mentionedUserIds(created)).containsExactly(3L);
    }

    private JsonNode postComment(String token, long ticketId, String content) {
        ResponseEntity<JsonNode> response = rest.postForEntity(
                "/tickets/" + ticketId + "/comments",
                new HttpEntity<>(Map.of("authorId", 1, "content", content), jsonBearer(token)),
                JsonNode.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        return response.getBody();
    }

    private Set<Long> mentionedUserIds(JsonNode comment) {
        Set<Long> ids = new HashSet<>();
        for (JsonNode m : comment.get("mentionedUsers")) {
            ids.add(m.get("id").asLong());
        }
        return ids;
    }
}
