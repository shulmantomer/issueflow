package com.att.tdp.issueflow.mention.dto;

import com.att.tdp.issueflow.comment.dto.CommentResponse;
import java.util.List;

public record UserMentionsResponse(List<CommentResponse> data, long total, int page) {}
