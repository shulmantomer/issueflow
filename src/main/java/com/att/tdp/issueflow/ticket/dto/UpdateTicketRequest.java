package com.att.tdp.issueflow.ticket.dto;

import com.att.tdp.issueflow.common.enums.TicketPriority;
import com.att.tdp.issueflow.common.enums.TicketStatus;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record UpdateTicketRequest(
        @Size(max = 255) String title,
        String description,
        TicketStatus status,
        TicketPriority priority,
        Long assigneeId,
        Instant dueDate
) {}
