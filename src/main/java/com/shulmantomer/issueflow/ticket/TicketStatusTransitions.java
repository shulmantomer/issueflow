package com.shulmantomer.issueflow.ticket;

import com.shulmantomer.issueflow.common.enums.TicketStatus;
import com.shulmantomer.issueflow.exception.ConflictException;

/**
 * Validates that a ticket status change is a legal one-step forward
 * transition: TODO → IN_PROGRESS → IN_REVIEW → DONE. Any skip-ahead,
 * backward, or same-status change throws {@link ConflictException} (req 2.4).
 */
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
