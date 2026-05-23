package com.att.tdp.issueflow.comment.dto;

import com.att.tdp.issueflow.user.User;

public record MentionedUser(Long id, String username, String fullName) {

    public static MentionedUser from(User user) {
        return new MentionedUser(user.getId(), user.getUsername(), user.getFullName());
    }
}
