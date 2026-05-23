package com.att.tdp.issueflow.comment;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @EntityGraph(attributePaths = "mentionedUsers")
    List<Comment> findByTicketIdOrderById(Long ticketId);

    @Query(value = "SELECT c FROM Comment c JOIN c.mentionedUsers u WHERE u.id = :userId "
                 + "ORDER BY c.createdAt DESC",
           countQuery = "SELECT count(c) FROM Comment c JOIN c.mentionedUsers u "
                      + "WHERE u.id = :userId")
    Page<Comment> findMentioning(@Param("userId") Long userId, Pageable pageable);
}
