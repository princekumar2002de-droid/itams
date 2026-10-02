package com.princekumar.itams.license;

import com.princekumar.itams.common.dto.PageResponse;
import com.princekumar.itams.license.dto.*;
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

@RestController
@RequestMapping("/api/v1/licenses")
@Tag(name = "Software Licenses", description = "Licenses + per-seat allocation")
public class SoftwareLicenseController {

    private final SoftwareLicenseService service;
    public SoftwareLicenseController(SoftwareLicenseService service) { this.service = service; }

    // ── licenses ─────────────────────────────────────────────────────────────

    @Operation(summary = "Register a new license (product is upserted by vendor+name+version)")
    @PreAuthorize("hasAnyRole('ADMIN','IT_MANAGER')")
    @PostMapping
    public ResponseEntity<LicenseResponse> create(@Valid @RequestBody CreateLicenseRequest req,
                                                  UriComponentsBuilder uri) {
        SoftwareLicense l = service.create(req);
        URI location = uri.path("/api/v1/licenses/{id}").buildAndExpand(l.getId()).toUri();
        return ResponseEntity.created(location).body(toResponse(l, service.countOpen(l.getId())));
    }

    @Operation(summary = "Get a license by id")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    public LicenseResponse get(@PathVariable Long id) {
        SoftwareLicense l = service.findById(id);
        return toResponse(l, service.countOpen(id));
    }

    @Operation(summary = "List licenses (paginated, optional search)")
    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public PageResponse<LicenseResponse> list(@RequestParam(required = false) String q, Pageable pageable) {
        return PageResponse.from(service.search(q, pageable), l -> toResponse(l, service.countOpen(l.getId())));
    }

    // ── assignments ──────────────────────────────────────────────────────────

    @Operation(summary = "Allocate one seat of this license to a person OR an asset")
    @PreAuthorize("hasAnyRole('ADMIN','IT_MANAGER')")
    @PostMapping("/{id}/assignments")
    public LicenseAssignmentResponse assign(@PathVariable Long id, @Valid @RequestBody AssignLicenseRequest req) {
        return toResponse(service.assign(id, req));
    }

    @Operation(summary = "List all assignments (past + present) for a license")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}/assignments")
    public List<LicenseAssignmentResponse> assignments(@PathVariable Long id) {
        return service.assignmentsFor(id).stream()
            .map(SoftwareLicenseController::toResponse)
            .toList();
    }

    @Operation(summary = "Release an allocated seat")
    @PreAuthorize("hasAnyRole('ADMIN','IT_MANAGER')")
    @PostMapping("/assignments/{assignmentId}/release")
    public LicenseAssignmentResponse release(@PathVariable Long assignmentId) {
        return toResponse(service.release(assignmentId));
    }

    // ── mappers (inline for this milestone) ─────────────────────────────────

    private static LicenseResponse toResponse(SoftwareLicense l, long used) {
        var p = l.getProduct();
        return new LicenseResponse(
            l.getId(),
            p.getId(), p.getVendor(), p.getName(), p.getVersion(),
            l.getLicenseReference(), l.getLicenseType(),
            l.getSeatsTotal(), (int) used, l.getSeatsTotal() - (int) used,
            l.getPurchaseDate(), l.getExpiresOn(),
            l.getCost(), l.getProcurementRef(),
            l.getCreatedAt(), l.getUpdatedAt()
        );
    }

    private static LicenseAssignmentResponse toResponse(LicenseAssignment la) {
        var p = la.getPerson();
        var a = la.getAsset();
        return new LicenseAssignmentResponse(
            la.getId(),
            la.getLicense().getId(),
            p != null ? p.getId() : null,
            p != null ? p.getFirstName() + " " + p.getLastName() : null,
            a != null ? a.getId() : null,
            a != null ? a.getAssetTag() : null,
            la.getAssignedAt(), la.getReleasedAt(),
            la.getNotes(), la.isOpen()
        );
    }
}
