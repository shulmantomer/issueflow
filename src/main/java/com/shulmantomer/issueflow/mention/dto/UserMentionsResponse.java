package com.shulmantomer.issueflow.mention.dto;

import com.shulmantomer.issueflow.comment.dto.CommentResponse;
import java.util.List;

public record UserMentionsResponse(List<CommentResponse> data, long total, int page) {}
