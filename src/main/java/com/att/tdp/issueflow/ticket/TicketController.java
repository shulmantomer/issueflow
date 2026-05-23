package com.att.tdp.issueflow.ticket;

import com.att.tdp.issueflow.security.AuthenticatedUser;
import com.att.tdp.issueflow.ticket.dto.CreateTicketRequest;
import com.att.tdp.issueflow.ticket.dto.TicketResponse;
import com.att.tdp.issueflow.ticket.dto.UpdateTicketRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping
    public ResponseEntity<List<TicketResponse>> getByProject(@RequestParam Long projectId) {
        return ResponseEntity.ok(ticketService.getByProject(projectId));
    }

    @GetMapping("/deleted")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<TicketResponse>> getDeleted(@RequestParam Long projectId) {
        return ResponseEntity.ok(ticketService.getDeletedByProject(projectId));
    }

    @GetMapping("/{ticketId}")
    public ResponseEntity<TicketResponse> getById(@PathVariable Long ticketId) {
        return ResponseEntity.ok(ticketService.getById(ticketId));
    }

    @PostMapping
    public ResponseEntity<TicketResponse> create(
            @Valid @RequestBody CreateTicketRequest request,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(ticketService.create(request, principal.id()));
    }

    @PatchMapping("/{ticketId}")
    public ResponseEntity<Void> update(
            @PathVariable Long ticketId,
            @Valid @RequestBody UpdateTicketRequest request,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        ticketService.update(ticketId, request, principal.id());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{ticketId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long ticketId,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        ticketService.delete(ticketId, principal.id());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{ticketId}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> restore(
            @PathVariable Long ticketId,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        ticketService.restore(ticketId, principal.id());
        return ResponseEntity.ok().build();
    }
}
