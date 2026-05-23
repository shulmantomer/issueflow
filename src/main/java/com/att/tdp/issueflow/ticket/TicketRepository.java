package com.att.tdp.issueflow.ticket;

import com.att.tdp.issueflow.common.enums.TicketPriority;
import com.att.tdp.issueflow.common.enums.TicketStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    List<Ticket> findByProjectIdOrderById(Long projectId);

    @Query("SELECT t FROM Ticket t WHERE t.dueDate IS NOT NULL AND t.dueDate < :now "
         + "AND t.status <> :done AND NOT (t.priority = :critical AND t.overdue = true)")
    List<Ticket> findEscalatable(@Param("now") Instant now,
                                 @Param("done") TicketStatus done,
                                 @Param("critical") TicketPriority critical);

    @Query(value = "SELECT * FROM tickets WHERE project_id = :projectId "
            + "AND deleted_at IS NOT NULL ORDER BY id", nativeQuery = true)
    List<Ticket> findDeletedByProjectId(@Param("projectId") Long projectId);

    @Query(value = "SELECT * FROM tickets WHERE id = :id AND deleted_at IS NOT NULL",
           nativeQuery = true)
    Optional<Ticket> findDeletedById(@Param("id") Long id);
}
