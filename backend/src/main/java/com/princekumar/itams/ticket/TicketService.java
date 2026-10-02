package com.princekumar.itams.ticket;

import com.princekumar.itams.asset.Asset;
import com.princekumar.itams.asset.AssetRepository;
import com.princekumar.itams.auth.CurrentUser;
import com.princekumar.itams.common.audit.AuditWrite;
import com.princekumar.itams.common.exception.BusinessRuleViolationException;
import com.princekumar.itams.common.exception.ResourceNotFoundException;
import com.princekumar.itams.person.Person;
import com.princekumar.itams.person.PersonRepository;
import com.princekumar.itams.ticket.dto.AddCommentRequest;
import com.princekumar.itams.ticket.dto.CreateTicketRequest;
import com.princekumar.itams.ticket.dto.UpdateTicketRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Business logic for support tickets.
 *
 * <h3>Row-level scoping for EMPLOYEE</h3>
 *
 * <p>The {@code POST /tickets} endpoint accepts {@code reporterPersonId} in the
 * request body. For a user whose only role is {@code EMPLOYEE}, the service
 * <b>ignores</b> the client-supplied value and binds the reporter to
 * {@link CurrentUser#personId()}, so an employee cannot raise a ticket on
 * behalf of someone else.</p>
 *
 * <p>{@link #search(String, TicketStatus, TicketPriority, Long, Long, Pageable)}
 * and {@link #findById(Long)} apply the same scoping — an EMPLOYEE only sees
 * tickets they reported or are explicitly assigned to; any other id resolves
 * to a 404 (we never leak existence of out-of-scope records).</p>
 *
 * <p>ADMIN and IT_MANAGER are unaffected: they continue to see all tickets.</p>
 */
@Service
@Transactional
public class TicketService {

    static final String EMPLOYEE_ROLE = "EMPLOYEE";

    private final TicketRepository repo;
    private final TicketCommentRepository commentRepo;
    private final PersonRepository personRepo;
    private final AssetRepository assetRepo;

    public TicketService(TicketRepository repo, TicketCommentRepository commentRepo,
                         PersonRepository personRepo, AssetRepository assetRepo) {
        this.repo = repo;
        this.commentRepo = commentRepo;
        this.personRepo = personRepo;
        this.assetRepo = assetRepo;
    }

    // ── create ───────────────────────────────────────────────────────────────

    @AuditWrite(entity = "Ticket", action = "CREATE")
    public Ticket create(CreateTicketRequest req) {
        long reporterPersonId = resolveReporterPersonId(req.reporterPersonId());

        Person reporter = personRepo.findByIdAndDeletedAtIsNull(reporterPersonId)
            .orElseThrow(() -> new ResourceNotFoundException("Person", reporterPersonId));

        Asset asset = null;
        if (req.relatedAssetId() != null) {
            asset = assetRepo.findById(req.relatedAssetId())
                .orElseThrow(() -> new ResourceNotFoundException("Asset", req.relatedAssetId()));
        }

        Ticket t = new Ticket(req.subject(), req.description(), req.priority(), reporter, asset);
        // Two-step number generation: initial UUID placeholder (satisfies the NOT NULL unique constraint),
        // then update to the friendly TCK-YYYY-000123 format once we have the DB id.
        t.setTicketNumber("PENDING-" + UUID.randomUUID().toString().substring(0, 8));
        Ticket saved = repo.save(t);
        saved.setTicketNumber("TCK-%d-%06d".formatted(
            saved.getCreatedAt() != null ? saved.getCreatedAt().getYear() : 2026,
            saved.getId()));
        return saved;   // dirty check triggers UPDATE on commit
    }

    /**
     * For an EMPLOYEE-only caller, the reporter is server-forced to their own
     * {@code person_id}. For ADMIN/IT_MANAGER (or when running without an
     * authenticated principal, e.g. in a unit test) the client-supplied value
     * is honoured. {@code null} always falls back to the current person id.
     */
    long resolveReporterPersonId(Long clientSuppliedPersonId) {
        if (CurrentUser.hasOnlyRole(EMPLOYEE_ROLE)) {
            return CurrentUser.personId();
        }
        return clientSuppliedPersonId != null ? clientSuppliedPersonId : CurrentUser.personId();
    }

    // ── update / status ──────────────────────────────────────────────────────

    @AuditWrite(entity = "Ticket", action = "UPDATE")
    public Ticket update(Long id, UpdateTicketRequest req) {
        Ticket t = requireById(id);
        if (req.priority() != null)         t.setPriority(req.priority());
        if (req.assignedToUserId() != null) t.setAssignedToUserId(req.assignedToUserId());
        return t;
    }

    @AuditWrite(entity = "Ticket", action = "STATUS_CHANGE")
    public Ticket transitionStatus(Long id, TicketStatus next) {
        Ticket t = requireById(id);
        try { t.transitionTo(next); }
        catch (IllegalStateException ex) {
            throw new BusinessRuleViolationException("ticket.illegal_transition", ex.getMessage());
        }
        return t;
    }

    // ── comments ─────────────────────────────────────────────────────────────

    @AuditWrite(entity = "TicketComment", action = "CREATE")
    public TicketComment addComment(Long ticketId, AddCommentRequest req) {
        Ticket t = requireById(ticketId);
        long author = CurrentUser.id();
        // EMPLOYEE-authored comments can never be internal — only staff can mark a comment as internal.
        boolean internal = req.internal();
        if (CurrentUser.hasOnlyRole(EMPLOYEE_ROLE) && internal) {
            internal = false;
        }
        return commentRepo.save(new TicketComment(t, author, req.body(), internal));
    }

    @Transactional(readOnly = true)
    public List<TicketComment> commentsOf(Long ticketId) {
        // EMPLOYEE should never see internal comments on any ticket — including
        // their own. requireById already enforces visibility of the ticket itself.
        requireById(ticketId);
        List<TicketComment> all = commentRepo.findByTicketId(ticketId);
        if (CurrentUser.hasOnlyRole(EMPLOYEE_ROLE)) {
            return all.stream().filter(c -> !c.isInternal()).toList();
        }
        return all;
    }

    // ── queries ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Ticket findById(Long id) { return requireById(id); }

    @Transactional(readOnly = true)
    public Page<Ticket> search(String q, TicketStatus status, TicketPriority priority,
                               Long assignedToUserId, Long reporterPersonId, Pageable pageable) {
        // EMPLOYEE-only callers always see their own tickets regardless of the
        // reporterPersonId filter the client sent — we overwrite it.
        if (CurrentUser.hasOnlyRole(EMPLOYEE_ROLE)) {
            long me = CurrentUser.personId();
            reporterPersonId = me;
        }
        return repo.search(q, status, priority, assignedToUserId, reporterPersonId, pageable);
    }

    /**
     * Resolve a ticket by id with row-level ownership scoping. For an
     * EMPLOYEE-only caller, any ticket they neither reported nor are
     * assigned to resolves to a 404 — we never leak existence of records
     * outside the caller's scope.
     */
    Ticket requireById(Long id) {
        Ticket t = repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Ticket", id));
        if (CurrentUser.hasOnlyRole(EMPLOYEE_ROLE) && !isVisibleToCurrentEmployee(t)) {
            throw new ResourceNotFoundException("Ticket", id);
        }
        return t;
    }

    private boolean isVisibleToCurrentEmployee(Ticket t) {
        long me = CurrentUser.personId();
        long myUserId = CurrentUser.id();
        boolean reporterMatch = t.getReporter() != null
            && t.getReporter().getId() != null
            && t.getReporter().getId().equals(me);
        boolean assigneeMatch = t.getAssignedToUserId() != null
            && t.getAssignedToUserId().equals(myUserId);
        return reporterMatch || assigneeMatch;
    }
}
