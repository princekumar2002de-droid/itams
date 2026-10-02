package com.princekumar.itams.maintenance;

import com.princekumar.itams.common.dto.PageResponse;
import com.princekumar.itams.maintenance.dto.CreateMaintenanceRequest;
import com.princekumar.itams.maintenance.dto.MaintenanceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/maintenance")
@Tag(name = "Maintenance", description = "Repair and service records against assets")
public class MaintenanceController {

    private final MaintenanceService service;
    public MaintenanceController(MaintenanceService service) { this.service = service; }

    @Operation(summary = "Record a maintenance event against an asset")
    @PreAuthorize("hasAnyRole('ADMIN','IT_MANAGER')")
    @PostMapping
    public ResponseEntity<MaintenanceResponse> create(@Valid @RequestBody CreateMaintenanceRequest req,
                                                      UriComponentsBuilder uri) {
        MaintenanceRecord m = service.create(req);
        URI location = uri.path("/api/v1/maintenance/{id}").buildAndExpand(m.getId()).toUri();
        return ResponseEntity.created(location).body(toResponse(m));
    }

    @Operation(summary = "Get a maintenance record by id")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    public MaintenanceResponse get(@PathVariable Long id) {
        return toResponse(service.findById(id));
    }

    @Operation(summary = "List / filter maintenance records")
    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public PageResponse<MaintenanceResponse> list(@RequestParam(required = false) String q,
                                                  @RequestParam(required = false) Long assetId,
                                                  Pageable pageable) {
        return PageResponse.from(service.search(q, assetId, pageable), MaintenanceController::toResponse);
    }

    @Operation(summary = "Delete a maintenance record")
    @PreAuthorize("hasAnyRole('ADMIN','IT_MANAGER')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { service.delete(id); }

    private static MaintenanceResponse toResponse(MaintenanceRecord m) {
        return new MaintenanceResponse(
            m.getId(),
            m.getAsset().getId(), m.getAsset().getAssetTag(),
            m.getPerformedOn(), m.getPerformedByUserId(),
            m.getProviderName(), m.getDescription(),
            m.getCost(), m.getNextScheduledOn(),
            m.getCreatedAt(), m.getUpdatedAt()
        );
    }
}
