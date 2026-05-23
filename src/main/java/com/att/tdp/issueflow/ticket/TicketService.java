package com.att.tdp.issueflow.ticket;

import com.att.tdp.issueflow.common.audit.AuditPublisher;
import com.att.tdp.issueflow.common.enums.AuditAction;
import com.att.tdp.issueflow.common.enums.AuditActor;
import com.att.tdp.issueflow.common.enums.AuditEntityType;
import com.att.tdp.issueflow.common.enums.TicketStatus;
import com.att.tdp.issueflow.exception.BadRequestException;
import com.att.tdp.issueflow.exception.ConflictException;
import com.att.tdp.issueflow.exception.NotFoundException;
import com.att.tdp.issueflow.project.ProjectRepository;
import com.att.tdp.issueflow.ticket.dependency.TicketDependency;
import com.att.tdp.issueflow.ticket.dependency.TicketDependencyRepository;
import com.att.tdp.issueflow.ticket.dto.CreateTicketRequest;
import com.att.tdp.issueflow.ticket.dto.TicketResponse;
import com.att.tdp.issueflow.ticket.dto.UpdateTicketRequest;
import com.att.tdp.issueflow.user.UserRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The core ticket business logic. Enforces the strict one-step status workflow
 * (TODO → IN_PROGRESS → IN_REVIEW → DONE), the DONE-lock, the
 * dependency-blocks-DONE guard, optimistic locking via {@code @Version}, soft
 * delete, the {@code is_overdue} reset on manual priority change, and
 * auto-assignment of unassigned tickets to the least-loaded developer
 * (tie-broken by user id).
 */
@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final TicketDependencyRepository dependencyRepository;
    private final AuditPublisher auditPublisher;

    public TicketService(TicketRepository ticketRepository,
                         ProjectRepository projectRepository,
                         UserRepository userRepository,
                         TicketDependencyRepository dependencyRepository,
                         AuditPublisher auditPublisher) {
        this.ticketRepository = ticketRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.dependencyRepository = dependencyRepository;
        this.auditPublisher = auditPublisher;
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> getByProject(Long projectId) {
        requireProjectExists(projectId);
        return ticketRepository.findByProjectIdOrderById(projectId).stream()
                .map(TicketResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public TicketResponse getById(Long id) {
        return TicketResponse.from(getTicketOrThrow(id));
    }

    @Transactional
    public TicketResponse create(CreateTicketRequest request, Long actorId) {
        if (!projectRepository.existsById(request.projectId())) {
            throw new BadRequestException("Project " + request.projectId() + " does not exist");
        }
        if (request.assigneeId() != null && !userRepository.existsById(request.assigneeId())) {
            throw new BadRequestException(
                    "Assignee user " + request.assigneeId() + " does not exist");
        }

        Long assigneeId = request.assigneeId();
        boolean autoAssigned = false;
        if (assigneeId == null) {
            assigneeId = autoAssignLeastLoadedDeveloper(request.projectId());
            autoAssigned = assigneeId != null;
        }

        Ticket ticket = Ticket.builder()
                .title(request.title())
                .description(request.description())
                .status(request.status())
                .priority(request.priority())
                .type(request.type())
                .projectId(request.projectId())
                .assigneeId(assigneeId)
                .dueDate(request.dueDate())
                .overdue(false)
                .build();
        Ticket saved = ticketRepository.save(ticket);

        auditPublisher.publish(AuditAction.CREATE, AuditEntityType.TICKET, saved.getId(),
                AuditActor.USER, actorId);
        if (autoAssigned) {
            auditPublisher.publish(AuditAction.AUTO_ASSIGN, AuditEntityType.TICKET,
                    saved.getId(), AuditActor.SYSTEM, null);
        }
        return TicketResponse.from(saved);
    }

    @Transactional
    public void update(Long id, UpdateTicketRequest request, Long actorId) {
        Ticket ticket = getTicketOrThrow(id);

        if (ticket.getStatus() == TicketStatus.DONE) {
            throw new ConflictException(
                    "Ticket " + id + " is DONE and can no longer be updated");
        }

        if (request.title() != null) {
            if (request.title().isBlank()) {
                throw new BadRequestException("Ticket title must not be blank");
            }
            ticket.setTitle(request.title());
        }
        if (request.description() != null) {
            ticket.setDescription(request.description());
        }
        if (request.status() != null && request.status() != ticket.getStatus()) {
            TicketStatusTransitions.assertAllowed(ticket.getStatus(), request.status());
            if (request.status() == TicketStatus.DONE) {
                assertNotBlocked(id);
            }
            ticket.setStatus(request.status());
        }
        if (request.priority() != null && request.priority() != ticket.getPriority()) {
            // Manual priority change resets auto-escalation state (req 3.7).
            ticket.setOverdue(false);
            ticket.setPriority(request.priority());
        }
        if (request.assigneeId() != null) {
            if (!userRepository.existsById(request.assigneeId())) {
                throw new BadRequestException(
                        "Assignee user " + request.assigneeId() + " does not exist");
            }
            ticket.setAssigneeId(request.assigneeId());
        }
        if (request.dueDate() != null) {
            ticket.setDueDate(request.dueDate());
        }

        ticketRepository.save(ticket);
        auditPublisher.publish(AuditAction.UPDATE, AuditEntityType.TICKET, id,
                AuditActor.USER, actorId);
    }

    @Transactional
    public void delete(Long id, Long actorId) {
        Ticket ticket = getTicketOrThrow(id);
        // Soft delete only — set the tombstone, never ticketRepository.delete().
        ticket.setDeletedAt(Instant.now());
        ticketRepository.save(ticket);
        auditPublisher.publish(AuditAction.DELETE, AuditEntityType.TICKET, id,
                AuditActor.USER, actorId);
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> getDeletedByProject(Long projectId) {
        requireProjectExists(projectId);
        return ticketRepository.findDeletedByProjectId(projectId).stream()
                .map(TicketResponse::from).toList();
    }

    @Transactional
    public void restore(Long id, Long actorId) {
        Ticket ticket = ticketRepository.findDeletedById(id)
                .orElseThrow(() -> new NotFoundException("Soft-deleted ticket " + id + " not found"));
        ticket.setDeletedAt(null);
        ticketRepository.save(ticket);
        auditPublisher.publish(AuditAction.RESTORE, AuditEntityType.TICKET, id,
                AuditActor.USER, actorId);
    }

    private Ticket getTicketOrThrow(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Ticket " + id + " not found"));
    }

    private void requireProjectExists(Long projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new NotFoundException("Project " + projectId + " not found");
        }
    }

    private Long autoAssignLeastLoadedDeveloper(Long projectId) {
        List<Object[]> workload = userRepository.findDeveloperWorkload(projectId);
        if (workload.isEmpty()) {
            return null; // no DEVELOPER users — ticket stays unassigned (req 3.8)
        }
        // Query is ordered by open-ticket count ASC, then user id ASC — first row wins.
        return ((Number) workload.get(0)[0]).longValue();
    }

    private void assertNotBlocked(Long ticketId) {
        List<Long> blockerIds = dependencyRepository.findByTicketId(ticketId).stream()
                .map(TicketDependency::getBlockedById).toList();
        if (blockerIds.isEmpty()) {
            return;
        }
        List<Long> unresolved = ticketRepository.findAllById(blockerIds).stream()
                .filter(t -> t.getStatus() != TicketStatus.DONE)
                .map(Ticket::getId)
                .sorted()
                .toList();
        if (!unresolved.isEmpty()) {
            throw new ConflictException("Ticket " + ticketId
                    + " cannot move to DONE — blocked by unresolved ticket(s): " + unresolved);
        }
    }
}
