package com.att.tdp.issueflow.ticket.dependency;

import com.att.tdp.issueflow.common.audit.AuditPublisher;
import com.att.tdp.issueflow.common.enums.AuditAction;
import com.att.tdp.issueflow.common.enums.AuditActor;
import com.att.tdp.issueflow.common.enums.AuditEntityType;
import com.att.tdp.issueflow.exception.BadRequestException;
import com.att.tdp.issueflow.exception.ConflictException;
import com.att.tdp.issueflow.exception.NotFoundException;
import com.att.tdp.issueflow.ticket.Ticket;
import com.att.tdp.issueflow.ticket.TicketRepository;
import com.att.tdp.issueflow.ticket.dependency.dto.DependencyResponse;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DependencyService {

    private final TicketDependencyRepository dependencyRepository;
    private final TicketRepository ticketRepository;
    private final AuditPublisher auditPublisher;

    public DependencyService(TicketDependencyRepository dependencyRepository,
                             TicketRepository ticketRepository,
                             AuditPublisher auditPublisher) {
        this.dependencyRepository = dependencyRepository;
        this.ticketRepository = ticketRepository;
        this.auditPublisher = auditPublisher;
    }

    @Transactional
    public void addDependency(Long ticketId, Long blockedById, Long actorId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new NotFoundException("Ticket " + ticketId + " not found"));
        Ticket blocker = ticketRepository.findById(blockedById)
                .orElseThrow(() -> new BadRequestException(
                        "Blocker ticket " + blockedById + " does not exist"));

        if (ticketId.equals(blockedById)) {
            throw new BadRequestException("A ticket cannot depend on itself");
        }
        if (!ticket.getProjectId().equals(blocker.getProjectId())) {
            throw new BadRequestException("Both tickets must belong to the same project");
        }
        if (dependencyRepository.existsByTicketIdAndBlockedById(ticketId, blockedById)) {
            throw new ConflictException("Ticket " + ticketId
                    + " is already blocked by ticket " + blockedById);
        }
        assertNoCycle(ticketId, blockedById);

        dependencyRepository.save(new TicketDependency(ticketId, blockedById));
        auditPublisher.publish(AuditAction.CREATE, AuditEntityType.TICKET_DEPENDENCY,
                ticketId, AuditActor.USER, actorId);
    }

    @Transactional(readOnly = true)
    public List<DependencyResponse> listDependencies(Long ticketId) {
        if (!ticketRepository.existsById(ticketId)) {
            throw new NotFoundException("Ticket " + ticketId + " not found");
        }
        List<Long> blockerIds = dependencyRepository.findByTicketId(ticketId).stream()
                .map(TicketDependency::getBlockedById).toList();
        return ticketRepository.findAllById(blockerIds).stream()
                .map(DependencyResponse::from).toList();
    }

    @Transactional
    public void removeDependency(Long ticketId, Long blockerId, Long actorId) {
        if (!dependencyRepository.existsByTicketIdAndBlockedById(ticketId, blockerId)) {
            throw new NotFoundException("Ticket " + ticketId
                    + " is not blocked by ticket " + blockerId);
        }
        dependencyRepository.deleteById(new TicketDependencyId(ticketId, blockerId));
        auditPublisher.publish(AuditAction.DELETE, AuditEntityType.TICKET_DEPENDENCY,
                ticketId, AuditActor.USER, actorId);
    }

    private void assertNoCycle(Long ticketId, Long blockedById) {
        // Adding "ticketId depends on blockedById". A cycle forms iff blockedById
        // already (transitively) depends on ticketId. DFS the existing graph.
        Set<Long> visited = new HashSet<>();
        Deque<Long> stack = new ArrayDeque<>();
        stack.push(blockedById);
        while (!stack.isEmpty()) {
            Long current = stack.pop();
            if (current.equals(ticketId)) {
                throw new ConflictException(
                        "Adding this dependency would create a circular dependency");
            }
            if (!visited.add(current)) {
                continue;
            }
            for (TicketDependency edge : dependencyRepository.findByTicketId(current)) {
                stack.push(edge.getBlockedById());
            }
        }
    }
}
