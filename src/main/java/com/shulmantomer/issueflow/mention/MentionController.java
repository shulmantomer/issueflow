package com.shulmantomer.issueflow.mention;

import com.shulmantomer.issueflow.comment.CommentService;
import com.shulmantomer.issueflow.mention.dto.UserMentionsResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MentionController {

    private final CommentService commentService;

    public MentionController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping("/users/{userId}/mentions")
    public ResponseEntity<UserMentionsResponse> getMentions(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ResponseEntity.ok(commentService.getMentionsForUser(userId, page, pageSize));
    }
}
