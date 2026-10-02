package com.princekumar.itams.asset;

import com.princekumar.itams.asset.dto.AssetCategoryResponse;
import com.princekumar.itams.common.exception.ResourceNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/asset-categories")
@Tag(name = "Asset Categories", description = "Reference data — read only for all roles")
public class AssetCategoryController {

    private final AssetCategoryRepository repo;
    public AssetCategoryController(AssetCategoryRepository repo) { this.repo = repo; }

    @Operation(summary = "List all asset categories")
    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public List<AssetCategoryResponse> list() {
        return repo.findAll(Sort.by("code")).stream()
            .map(c -> new AssetCategoryResponse(c.getId(), c.getCode(), c.getName(), c.getDescription()))
            .toList();
    }

    @Operation(summary = "Get one asset category by id")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    public AssetCategoryResponse get(@PathVariable Long id) {
        var c = repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("AssetCategory", id));
        return new AssetCategoryResponse(c.getId(), c.getCode(), c.getName(), c.getDescription());
    }
}
