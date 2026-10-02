package com.princekumar.itams.asset;

import com.princekumar.itams.asset.dto.AssetModelCreateRequest;
import com.princekumar.itams.common.exception.BusinessRuleViolationException;
import com.princekumar.itams.common.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AssetModelService {

    private final AssetModelRepository repo;
    private final AssetCategoryRepository categoryRepo;

    public AssetModelService(AssetModelRepository repo, AssetCategoryRepository categoryRepo) {
        this.repo = repo;
        this.categoryRepo = categoryRepo;
    }

    public AssetModel create(AssetModelCreateRequest req) {
        if (repo.existsByManufacturerAndModelName(req.manufacturer(), req.modelName())) {
            throw new BusinessRuleViolationException(
                "asset_model.duplicate",
                "Asset model '%s %s' already exists.".formatted(req.manufacturer(), req.modelName()));
        }
        AssetCategory cat = categoryRepo.findById(req.categoryId())
            .orElseThrow(() -> new ResourceNotFoundException("AssetCategory", req.categoryId()));
        return repo.save(new AssetModel(cat, req.manufacturer(), req.modelName(), req.specs()));
    }

    @Transactional(readOnly = true)
    public AssetModel findById(Long id) {
        return repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("AssetModel", id));
    }

    @Transactional(readOnly = true)
    public Page<AssetModel> search(String q, Long categoryId, Pageable pageable) {
        return repo.search(q, categoryId, pageable);
    }

    AssetModel requireById(Long id) { return findById(id); }
}
