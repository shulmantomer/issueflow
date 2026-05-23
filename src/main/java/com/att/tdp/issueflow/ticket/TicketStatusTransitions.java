package com.att.tdp.issueflow.ticket;

import com.att.tdp.issueflow.common.enums.TicketStatus;
import com.att.tdp.issueflow.exception.ConflictException;

public final class TicketStatusTransitions {

    private TicketStatusTransitions() {
    }

    public static void assertAllowed(TicketStatus current, TicketStatus next) {
        if (next.ordinal() != current.ordinal() + 1) {
            throw new ConflictException(
                    "Invalid status transition " + current + " -> " + next
                    + ". Status may only advance one step: "
                    + "TODO -> IN_PROGRESS -> IN_REVIEW -> DONE.");
        }
    }
}
