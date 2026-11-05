package com.shulmantomer.issueflow.ticket.dto;

import com.shulmantomer.issueflow.common.enums.TicketPriority;
import com.shulmantomer.issueflow.common.enums.TicketStatus;
import com.shulmantomer.issueflow.common.enums.TicketType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record CreateTicketRequest(
        @NotBlank @Size(max = 255) String title,
        String description,
        @NotNull TicketStatus status,
        @NotNull TicketPriority priority,
        @NotNull TicketType type,
        @NotNull Long projectId,
        Long assigneeId,
        Instant dueDate
) {}
