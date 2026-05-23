package com.att.tdp.issueflow.ticket.dependency;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ticket_dependencies")
@IdClass(TicketDependencyId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TicketDependency {

    @Id
    @Column(name = "ticket_id")
    private Long ticketId;

    @Id
    @Column(name = "blocked_by_id")
    private Long blockedById;
}
