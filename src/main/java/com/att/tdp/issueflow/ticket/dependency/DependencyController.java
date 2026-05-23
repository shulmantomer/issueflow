package com.att.tdp.issueflow.ticket.dependency;

import com.att.tdp.issueflow.security.AuthenticatedUser;
import com.att.tdp.issueflow.ticket.dependency.dto.AddDependencyRequest;
import com.att.tdp.issueflow.ticket.dependency.dto.DependencyResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tickets/{ticketId}/dependencies")
public class DependencyController {

    private final DependencyService dependencyService;

    public DependencyController(DependencyService dependencyService) {
        this.dependencyService = dependencyService;
    }

    @GetMapping
    public ResponseEntity<List<DependencyResponse>> list(@PathVariable Long ticketId) {
        return ResponseEntity.ok(dependencyService.listDependencies(ticketId));
    }

    @PostMapping
    public ResponseEntity<Void> add(
            @PathVariable Long ticketId,
            @Valid @RequestBody AddDependencyRequest request,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        dependencyService.addDependency(ticketId, request.blockedBy(), principal.id());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{blockerId}")
    public ResponseEntity<Void> remove(
            @PathVariable Long ticketId,
            @PathVariable Long blockerId,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        dependencyService.removeDependency(ticketId, blockerId, principal.id());
        return ResponseEntity.ok().build();
    }
}
