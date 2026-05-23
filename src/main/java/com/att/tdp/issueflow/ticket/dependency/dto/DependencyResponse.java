package com.att.tdp.issueflow.ticket.dependency.dto;

import com.att.tdp.issueflow.common.enums.TicketStatus;
import com.att.tdp.issueflow.ticket.Ticket;

public record DependencyResponse(Long id, String title, TicketStatus status) {

    public static DependencyResponse from(Ticket ticket) {
        return new DependencyResponse(ticket.getId(), ticket.getTitle(), ticket.getStatus());
    }
}
