package com.att.tdp.issueflow.mention;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts {@code @username} mentions from comment text via a word-character
 * regex, returning a {@link java.util.LinkedHashSet} for deterministic dedup
 * order. Case-insensitive matching against real users happens later in the
 * service (req 3.6).
 */
public final class MentionExtractor {

    private static final Pattern MENTION_PATTERN = Pattern.compile("@([A-Za-z0-9_]+)");

    private MentionExtractor() {
    }

    public static Set<String> extractUsernames(String content) {
        Set<String> usernames = new LinkedHashSet<>();
        if (content == null) {
            return usernames;
        }
        Matcher matcher = MENTION_PATTERN.matcher(content);
        while (matcher.find()) {
            usernames.add(matcher.group(1));
        }
        return usernames;
    }
}
