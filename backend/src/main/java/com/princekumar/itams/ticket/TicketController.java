package com.princekumar.itams.ticket;

import com.princekumar.itams.common.dto.PageResponse;
import com.princekumar.itams.ticket.dto.*;
import com.princekumar.itams.user.UserDisplayNames;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/v1/tickets")
@Tag(name = "Support Tickets", description = "Employee-raised tickets, state machine, comments")
public class TicketController {

    private final TicketService service;
    private final UserDisplayNames names;

    public TicketController(TicketService service, UserDisplayNames names) {
        this.service = service;
        this.names = names;
    }

    // ── ticket CRUD ──────────────────────────────────────────────────────────

    @Operation(summary = "Raise a new ticket")
    @PreAuthorize("isAuthenticated()")
    @PostMapping
    public ResponseEntity<TicketResponse> create(@Valid @RequestBody CreateTicketRequest req,
                                                 UriComponentsBuilder uri) {
        Ticket t = service.create(req);
        URI location = uri.path("/api/v1/tickets/{id}").buildAndExpand(t.getId()).toUri();
        return ResponseEntity.created(location).body(toResponse(t, service.commentsOf(t.getId())));
    }

    @Operation(summary = "Get a ticket by id (with its comments)")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    public TicketResponse get(@PathVariable Long id) {
        Ticket t = service.findById(id);
        return toResponse(t, service.commentsOf(id));
    }

    @Operation(summary = "List / filter tickets")
    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public PageResponse<TicketResponse> list(@RequestParam(required = false) String q,
                                             @RequestParam(required = false) TicketStatus status,
                                             @RequestParam(required = false) TicketPriority priority,
                                             @RequestParam(required = false) Long assignedToUserId,
                                             @RequestParam(required = false) Long reporterPersonId,
                                             Pageable pageable) {
        var page = service.search(q, status, priority, assignedToUserId, reporterPersonId, pageable);
        var userNames = names.of(page.getContent().stream().map(Ticket::getAssignedToUserId).toList());
        return PageResponse.from(page, t -> toResponseNoComments(t, userNames));
    }

    @Operation(summary = "Update a ticket (priority / assignee)")
    @PreAuthorize("hasAnyRole('ADMIN','IT_MANAGER')")
    @PatchMapping("/{id}")
    public TicketResponse update(@PathVariable Long id, @Valid @RequestBody UpdateTicketRequest req) {
        Ticket t = service.update(id, req);
        return toResponseNoComments(t, names.of(singleId(t.getAssignedToUserId())));
    }

    @Operation(summary = "Transition ticket status (state machine)")
    @PreAuthorize("hasAnyRole('ADMIN','IT_MANAGER')")
    @PostMapping("/{id}/status")
    public TicketResponse changeStatus(@PathVariable Long id, @Valid @RequestBody StatusChangeRequest req) {
        Ticket t = service.transitionStatus(id, req.to());
        return toResponseNoComments(t, names.of(singleId(t.getAssignedToUserId())));
    }

    // ── comments ─────────────────────────────────────────────────────────────

    @Operation(summary = "Add a comment to a ticket (internal or public)")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{id}/comments")
    public TicketCommentResponse addComment(@PathVariable Long id, @Valid @RequestBody AddCommentRequest req) {
        TicketComment c = service.addComment(id, req);
        return toCommentResponse(c, names.of(singleId(c.getAuthorUserId())));
    }

    // ── mappers (inline) ────────────────────────────────────────────────────

    /** List.of() rejects null, and a ticket may have no assignee. */
    private static List<Long> singleId(Long id) {
        return id == null ? List.of() : List.of(id);
    }

    private TicketResponse toResponse(Ticket t, List<TicketComment> comments) {
        var userNames = names.of(Stream.concat(
            Stream.of(t.getAssignedToUserId()),
            comments.stream().map(TicketComment::getAuthorUserId)).toList());
        var r = toResponseNoComments(t, userNames);
        return new TicketResponse(
            r.id(), r.ticketNumber(), r.subject(), r.description(), r.priority(), r.status(),
            r.reporterPersonId(), r.reporterName(),
            r.relatedAssetId(), r.relatedAssetTag(),
            r.assignedToUserId(), r.assignedToName(), r.resolvedAt(), r.closedAt(),
            r.createdAt(), r.updatedAt(),
            comments.stream().map(c -> toCommentResponse(c, userNames)).toList()
        );
    }

    private static TicketResponse toResponseNoComments(Ticket t, Map<Long, String> userNames) {
        var reporter = t.getReporter();
        var asset = t.getRelatedAsset();
        return new TicketResponse(
            t.getId(), t.getTicketNumber(), t.getSubject(), t.getDescription(),
            t.getPriority(), t.getStatus(),
            reporter.getId(), reporter.getFirstName() + " " + reporter.getLastName(),
            asset != null ? asset.getId() : null,
            asset != null ? asset.getAssetTag() : null,
            t.getAssignedToUserId(), userNames.get(t.getAssignedToUserId()),
            t.getResolvedAt(), t.getClosedAt(),
            t.getCreatedAt(), t.getUpdatedAt(),
            null
        );
    }

    private static TicketCommentResponse toCommentResponse(TicketComment c, Map<Long, String> userNames) {
        return new TicketCommentResponse(
            c.getId(), c.getAuthorUserId(), userNames.get(c.getAuthorUserId()),
            c.getBody(), c.isInternal(), c.getCreatedAt()
        );
    }
}
