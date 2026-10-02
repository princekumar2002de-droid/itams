package com.princekumar.itams.asset;

import com.princekumar.itams.asset.dto.AssetModelCreateRequest;
import com.princekumar.itams.asset.dto.AssetModelResponse;
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
@RequestMapping("/api/v1/asset-models")
@Tag(name = "Asset Models", description = "Commercial models an asset can be one of")
public class AssetModelController {

    private final AssetModelService service;
    public AssetModelController(AssetModelService service) { this.service = service; }

    @Operation(summary = "Create an asset model")
    @PreAuthorize("hasAnyRole('ADMIN','IT_MANAGER')")
    @PostMapping
    public ResponseEntity<AssetModelResponse> create(@Valid @RequestBody AssetModelCreateRequest req, UriComponentsBuilder uri) {
        AssetModel m = service.create(req);
        URI location = uri.path("/api/v1/asset-models/{id}").buildAndExpand(m.getId()).toUri();
        return ResponseEntity.created(location).body(AssetModelMapper.toResponse(m));
    }

    @Operation(summary = "Get an asset model by id")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    public AssetModelResponse get(@PathVariable Long id) {
        return AssetModelMapper.toResponse(service.findById(id));
    }

    @Operation(summary = "List asset models (paginated, optional search + category filter)")
    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public PageResponse<AssetModelResponse> list(@RequestParam(required = false) String q,
                                                 @RequestParam(required = false) Long categoryId,
                                                 Pageable pageable) {
        return PageResponse.from(service.search(q, categoryId, pageable), AssetModelMapper::toResponse);
    }
}
