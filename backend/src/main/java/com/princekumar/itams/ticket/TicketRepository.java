package com.princekumar.itams.ticket;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    /**
     * Loads the associations the API response reads ("reporter", "relatedAsset") in the same query.
     * open-in-view is off, so anything not loaded here would throw
     * LazyInitializationException when the controller maps the entity.
     */
    @EntityGraph(attributePaths = {"reporter", "relatedAsset"})
    @Override
    Optional<Ticket> findById(Long id);

    @Query("""
        SELECT t FROM Ticket t
         WHERE (:status IS NULL OR t.status = :status)
           AND (:priority IS NULL OR t.priority = :priority)
           AND (:assignedToUserId IS NULL OR t.assignedToUserId = :assignedToUserId)
           AND (:reporterPersonId IS NULL OR t.reporter.id = :reporterPersonId)
           AND (:q IS NULL OR :q = ''
                OR LOWER(t.subject) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(t.ticketNumber) LIKE LOWER(CONCAT('%', :q, '%')))
        ORDER BY t.createdAt DESC
    """)
    @EntityGraph(attributePaths = {"reporter", "relatedAsset"})
    Page<Ticket> search(@Param("q") String q,
                        @Param("status") TicketStatus status,
                        @Param("priority") TicketPriority priority,
                        @Param("assignedToUserId") Long assignedToUserId,
                        @Param("reporterPersonId") Long reporterPersonId,
                        Pageable pageable);
}
