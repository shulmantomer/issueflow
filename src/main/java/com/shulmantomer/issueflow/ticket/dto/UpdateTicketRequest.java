package com.shulmantomer.issueflow.ticket.dto;

import com.shulmantomer.issueflow.common.enums.TicketPriority;
import com.shulmantomer.issueflow.common.enums.TicketStatus;
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
