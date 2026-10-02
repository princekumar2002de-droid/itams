package com.princekumar.itams.asset;

import com.princekumar.itams.asset.dto.AssetCreateRequest;
import com.princekumar.itams.asset.dto.AssetResponse;
import com.princekumar.itams.asset.dto.AssetRetireRequest;
import com.princekumar.itams.asset.dto.AssetUpdateRequest;
import com.princekumar.itams.common.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/assets")
@Tag(name = "Assets", description = "Individual physical assets — CRUD + retire")
public class AssetController {

    private final AssetService service;
    public AssetController(AssetService service) { this.service = service; }

    @Operation(summary = "Register a new asset (status = IN_STOCK)")
    @PreAuthorize("hasAnyRole('ADMIN','IT_MANAGER')")
    @PostMapping
    public ResponseEntity<AssetResponse> create(@Valid @RequestBody AssetCreateRequest req, UriComponentsBuilder uri) {
        Asset a = service.create(req);
        URI location = uri.path("/api/v1/assets/{id}").buildAndExpand(a.getId()).toUri();
        return ResponseEntity.created(location).body(AssetMapper.toResponse(a));
    }

    @Operation(summary = "Get an asset by id")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    public AssetResponse get(@PathVariable Long id) {
        return AssetMapper.toResponse(service.findById(id));
    }

    @Operation(summary = "List assets (paginated, filter by status / category / free-text)")
    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public PageResponse<AssetResponse> list(@RequestParam(required = false) String q,
                                            @RequestParam(required = false) AssetStatus status,
                                            @RequestParam(required = false) Long categoryId,
                                            Pageable pageable) {
        return PageResponse.from(service.search(q, status, categoryId, pageable), AssetMapper::toResponse);
    }

    @Operation(summary = "Update warranty / notes on an asset")
    @PreAuthorize("hasAnyRole('ADMIN','IT_MANAGER')")
    @PatchMapping("/{id}")
    public AssetResponse update(@PathVariable Long id, @Valid @RequestBody AssetUpdateRequest req) {
        return AssetMapper.toResponse(service.update(id, req));
    }

    @Operation(summary = "Retire an asset (terminal status; must not be currently assigned)")
    @PreAuthorize("hasAnyRole('ADMIN','IT_MANAGER')")
    @PostMapping("/{id}/retire")
    public AssetResponse retire(@PathVariable Long id, @Valid @RequestBody AssetRetireRequest req) {
        return AssetMapper.toResponse(service.retire(id, req));
    }

    @Operation(summary = "Mark maintenance finished: UNDER_MAINTENANCE → IN_STOCK")
    @PreAuthorize("hasAnyRole('ADMIN','IT_MANAGER')")
    @PostMapping("/{id}/maintenance-complete")
    public AssetResponse completeMaintenance(@PathVariable Long id) {
        return AssetMapper.toResponse(service.completeMaintenance(id));
    }
}
