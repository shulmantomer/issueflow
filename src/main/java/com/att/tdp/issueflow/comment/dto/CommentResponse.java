package com.att.tdp.issueflow.comment.dto;

import com.att.tdp.issueflow.comment.Comment;
import java.util.Comparator;
import java.util.List;

public record CommentResponse(
        Long id,
        Long ticketId,
        Long authorId,
        String content,
        List<MentionedUser> mentionedUsers
) {
    public static CommentResponse from(Comment comment) {
        List<MentionedUser> mentions = comment.getMentionedUsers().stream()
                .map(MentionedUser::from)
                .sorted(Comparator.comparing(MentionedUser::id))
                .toList();
        return new CommentResponse(comment.getId(), comment.getTicketId(),
                comment.getAuthorId(), comment.getContent(), mentions);
    }
}
