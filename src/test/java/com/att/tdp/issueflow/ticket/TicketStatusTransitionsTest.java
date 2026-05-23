package com.att.tdp.issueflow.ticket;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.att.tdp.issueflow.common.enums.TicketStatus;
import com.att.tdp.issueflow.exception.ConflictException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TicketStatusTransitionsTest {

    @ParameterizedTest
    @CsvSource({"TODO,IN_PROGRESS", "IN_PROGRESS,IN_REVIEW", "IN_REVIEW,DONE"})
    void allowsOneStepForward(TicketStatus current, TicketStatus next) {
        assertThatCode(() -> TicketStatusTransitions.assertAllowed(current, next))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @CsvSource({
            "TODO,IN_REVIEW", "TODO,DONE", "IN_PROGRESS,DONE",
            "IN_PROGRESS,TODO", "IN_REVIEW,IN_PROGRESS", "DONE,TODO",
            "TODO,TODO", "DONE,DONE"
    })
    void rejectsSkipBackwardAndSameStatus(TicketStatus current, TicketStatus next) {
        assertThatThrownBy(() -> TicketStatusTransitions.assertAllowed(current, next))
                .isInstanceOf(ConflictException.class);
    }
}
