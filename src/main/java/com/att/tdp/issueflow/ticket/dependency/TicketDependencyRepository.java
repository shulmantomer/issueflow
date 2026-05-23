package com.att.tdp.issueflow.ticket.dependency;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketDependencyRepository
        extends JpaRepository<TicketDependency, TicketDependencyId> {

    List<TicketDependency> findByTicketId(Long ticketId);

    boolean existsByTicketIdAndBlockedById(Long ticketId, Long blockedById);
}
