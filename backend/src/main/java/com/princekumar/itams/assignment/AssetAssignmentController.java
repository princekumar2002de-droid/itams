package com.princekumar.itams.assignment;

import com.princekumar.itams.assignment.dto.AssetAssignmentResponse;
import com.princekumar.itams.assignment.dto.AssignAssetRequest;
import com.princekumar.itams.assignment.dto.ReturnAssetRequest;
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
@RequestMapping("/api/v1/assignments")
@Tag(name = "Asset Assignments", description = "Assign / return assets. One open assignment per asset.")
public class AssetAssignmentController {

    private final AssetAssignmentService service;
    public AssetAssignmentController(AssetAssignmentService service) { this.service = service; }

    @Operation(summary = "Assign an asset to a person")
    @PreAuthorize("hasAnyRole('ADMIN','IT_MANAGER')")
    @PostMapping
    public ResponseEntity<AssetAssignmentResponse> assign(@Valid @RequestBody AssignAssetRequest req, UriComponentsBuilder uri) {
        AssetAssignment a = service.assign(req);
        URI location = uri.path("/api/v1/assignments/{id}").buildAndExpand(a.getId()).toUri();
        return ResponseEntity.created(location).body(AssetAssignmentMapper.toResponse(a));
    }

    @Operation(summary = "Close an assignment (return the asset)")
    @PreAuthorize("hasAnyRole('ADMIN','IT_MANAGER')")
    @PostMapping("/{id}/return")
    public AssetAssignmentResponse returnIt(@PathVariable Long id, @Valid @RequestBody ReturnAssetRequest req) {
        return AssetAssignmentMapper.toResponse(service.returnAssignment(id, req));
    }

    @Operation(summary = "Get one assignment by id")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    public AssetAssignmentResponse get(@PathVariable Long id) {
        return AssetAssignmentMapper.toResponse(service.findById(id));
    }

    @Operation(summary = "List assignments (filter by asset / person; onlyOpen=true excludes returned ones)")
    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public PageResponse<AssetAssignmentResponse> list(@RequestParam(required = false) Long assetId,
                                                      @RequestParam(required = false) Long personId,
                                                      @RequestParam(defaultValue = "false") boolean onlyOpen,
                                                      Pageable pageable) {
        return PageResponse.from(service.search(assetId, personId, onlyOpen, pageable),
                                 AssetAssignmentMapper::toResponse);
    }
}
