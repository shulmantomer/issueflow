package com.att.tdp.issueflow.ticket.dto;

import com.att.tdp.issueflow.common.enums.TicketPriority;
import com.att.tdp.issueflow.common.enums.TicketStatus;
import com.att.tdp.issueflow.common.enums.TicketType;
import com.att.tdp.issueflow.ticket.Ticket;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public record TicketResponse(
        Long id,
        String title,
        String description,
        TicketStatus status,
        TicketPriority priority,
        TicketType type,
        Long projectId,
        Long assigneeId,
        Instant dueDate,
        @JsonProperty("isOverdue") boolean isOverdue
) {
    public static TicketResponse from(Ticket ticket) {
        return new TicketResponse(
                ticket.getId(), ticket.getTitle(), ticket.getDescription(),
                ticket.getStatus(), ticket.getPriority(), ticket.getType(),
                ticket.getProjectId(), ticket.getAssigneeId(), ticket.getDueDate(),
                ticket.isOverdue());
    }
}
