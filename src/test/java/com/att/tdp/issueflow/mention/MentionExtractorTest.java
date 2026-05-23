package com.att.tdp.issueflow.mention;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MentionExtractorTest {

    @Test
    void extractsMultipleMentions() {
        assertThat(MentionExtractor.extractUsernames("hi @jdoe and @asmith here"))
                .containsExactlyInAnyOrder("jdoe", "asmith");
    }

    @Test
    void returnsEmptyWhenNoMentions() {
        assertThat(MentionExtractor.extractUsernames("nothing to see here")).isEmpty();
    }

    @Test
    void returnsEmptyForNullContent() {
        assertThat(MentionExtractor.extractUsernames(null)).isEmpty();
    }

    @Test
    void dedupesRepeatedMention() {
        assertThat(MentionExtractor.extractUsernames("@dev1 @dev1 @dev1"))
                .containsExactly("dev1");
    }

    @Test
    void stopsAtNonWordCharacter() {
        assertThat(MentionExtractor.extractUsernames("ping @developer2! now"))
                .containsExactly("developer2");
    }

    @Test
    void preservesCaseAsTyped() {
        // Case-insensitive matching happens at user resolution, not extraction.
        assertThat(MentionExtractor.extractUsernames("@DEV1 and @dev1"))
                .containsExactlyInAnyOrder("DEV1", "dev1");
    }
}
