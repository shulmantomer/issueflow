package com.att.tdp.issueflow.ticket.dependency;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class TicketDependencyId implements Serializable {

    private Long ticketId;
    private Long blockedById;
}
