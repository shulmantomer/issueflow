package com.att.tdp.issueflow.comment;

import com.att.tdp.issueflow.comment.dto.CommentResponse;
import com.att.tdp.issueflow.comment.dto.CreateCommentRequest;
import com.att.tdp.issueflow.comment.dto.UpdateCommentRequest;
import com.att.tdp.issueflow.common.audit.AuditPublisher;
import com.att.tdp.issueflow.common.enums.AuditAction;
import com.att.tdp.issueflow.common.enums.AuditActor;
import com.att.tdp.issueflow.common.enums.AuditEntityType;
import com.att.tdp.issueflow.exception.BadRequestException;
import com.att.tdp.issueflow.exception.NotFoundException;
import com.att.tdp.issueflow.mention.MentionExtractor;
import com.att.tdp.issueflow.mention.dto.UserMentionsResponse;
import com.att.tdp.issueflow.ticket.TicketRepository;
import com.att.tdp.issueflow.user.User;
import com.att.tdp.issueflow.user.UserRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final AuditPublisher auditPublisher;

    public CommentService(CommentRepository commentRepository,
                          TicketRepository ticketRepository,
                          UserRepository userRepository,
                          AuditPublisher auditPublisher) {
        this.commentRepository = commentRepository;
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.auditPublisher = auditPublisher;
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> getByTicket(Long ticketId) {
        requireTicketExists(ticketId);
        return commentRepository.findByTicketIdOrderById(ticketId).stream()
                .map(CommentResponse::from).toList();
    }

    @Transactional
    public CommentResponse create(Long ticketId, CreateCommentRequest request, Long actorId) {
        requireTicketExists(ticketId);
        if (!userRepository.existsById(request.authorId())) {
            throw new BadRequestException(
                    "Author user " + request.authorId() + " does not exist");
        }
        Comment comment = Comment.builder()
                .ticketId(ticketId)
                .authorId(request.authorId())
                .content(request.content())
                .build();
        comment.setMentionedUsers(resolveMentions(request.content()));
        Comment saved = commentRepository.save(comment);
        auditPublisher.publish(AuditAction.CREATE, AuditEntityType.COMMENT, saved.getId(),
                AuditActor.USER, actorId);
        return CommentResponse.from(saved);
    }

    @Transactional
    public void update(Long ticketId, Long commentId, UpdateCommentRequest request, Long actorId) {
        Comment comment = getCommentOnTicketOrThrow(ticketId, commentId);
        comment.setContent(request.content());
        // Re-evaluate mentions: newly added are created, removed are deleted (req 3.6).
        comment.setMentionedUsers(resolveMentions(request.content()));
        commentRepository.save(comment);
        auditPublisher.publish(AuditAction.UPDATE, AuditEntityType.COMMENT, commentId,
                AuditActor.USER, actorId);
    }

    @Transactional
    public void delete(Long ticketId, Long commentId, Long actorId) {
        Comment comment = getCommentOnTicketOrThrow(ticketId, commentId);
        // Hard delete — comments are not soft-deletable; comment_mentions cascades.
        commentRepository.delete(comment);
        auditPublisher.publish(AuditAction.DELETE, AuditEntityType.COMMENT, commentId,
                AuditActor.USER, actorId);
    }

    @Transactional(readOnly = true)
    public UserMentionsResponse getMentionsForUser(Long userId, int page, int pageSize) {
        if (page < 1) {
            throw new BadRequestException("page must be >= 1");
        }
        if (pageSize < 1) {
            throw new BadRequestException("pageSize must be >= 1");
        }
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("User " + userId + " not found");
        }
        Page<Comment> result = commentRepository.findMentioning(userId,
                PageRequest.of(page - 1, pageSize));
        List<CommentResponse> data = result.getContent().stream()
                .map(CommentResponse::from).toList();
        return new UserMentionsResponse(data, result.getTotalElements(), page);
    }

    private Set<User> resolveMentions(String content) {
        Set<User> users = new HashSet<>();
        for (String username : MentionExtractor.extractUsernames(content)) {
            userRepository.findByUsernameIgnoreCase(username).ifPresent(users::add);
        }
        return users;
    }

    private Comment getCommentOnTicketOrThrow(Long ticketId, Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Comment " + commentId + " not found"));
        if (!comment.getTicketId().equals(ticketId)) {
            throw new NotFoundException(
                    "Comment " + commentId + " not found on ticket " + ticketId);
        }
        return comment;
    }

    private void requireTicketExists(Long ticketId) {
        if (!ticketRepository.existsById(ticketId)) {
            throw new NotFoundException("Ticket " + ticketId + " not found");
        }
    }
}
