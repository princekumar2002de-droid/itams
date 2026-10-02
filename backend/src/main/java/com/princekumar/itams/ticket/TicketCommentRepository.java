package com.princekumar.itams.ticket;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TicketCommentRepository extends JpaRepository<TicketComment, Long> {

    @Query("SELECT c FROM TicketComment c WHERE c.ticket.id = :ticketId ORDER BY c.createdAt")
    List<TicketComment> findByTicketId(@Param("ticketId") Long ticketId);
}
