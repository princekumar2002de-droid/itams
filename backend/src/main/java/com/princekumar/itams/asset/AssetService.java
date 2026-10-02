package com.princekumar.itams.asset;

import com.princekumar.itams.common.audit.AuditWrite;
import com.princekumar.itams.asset.dto.AssetCreateRequest;
import com.princekumar.itams.asset.dto.AssetRetireRequest;
import com.princekumar.itams.asset.dto.AssetUpdateRequest;
import com.princekumar.itams.common.exception.BusinessRuleViolationException;
import com.princekumar.itams.common.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AssetService {

    private final AssetRepository repo;
    private final AssetModelRepository modelRepo;

    public AssetService(AssetRepository repo, AssetModelRepository modelRepo) {
        this.repo = repo;
        this.modelRepo = modelRepo;
    }

    @AuditWrite(entity = "Asset", action = "CREATE")
    public Asset create(AssetCreateRequest req) {
        if (repo.existsByAssetTag(req.assetTag())) {
            throw new BusinessRuleViolationException(
                "asset.tag_already_used",
                "Asset tag '%s' is already in use.".formatted(req.assetTag()));
        }
        AssetModel model = modelRepo.findById(req.modelId())
            .orElseThrow(() -> new ResourceNotFoundException("AssetModel", req.modelId()));

        Asset a = new Asset(
            req.assetTag(), model, req.serialNumber(),
            req.purchaseDate(), req.purchasePrice(),
            req.warrantyEndsOn(), req.notes()
        );
        return repo.save(a);
    }

    @AuditWrite(entity = "Asset", action = "UPDATE")
    public Asset update(Long id, AssetUpdateRequest req) {
        Asset a = requireById(id);
        if (a.getStatus() == AssetStatus.RETIRED) {
            throw new BusinessRuleViolationException(
                "asset.retired_immutable", "Retired assets cannot be edited.");
        }
        if (req.warrantyEndsOn() != null) a.setWarrantyEndsOn(req.warrantyEndsOn());
        if (req.notes() != null) a.setNotes(req.notes());
        return a;
    }

    @AuditWrite(entity = "Asset", action = "STATUS_CHANGE")
    public Asset retire(Long id, AssetRetireRequest req) {
        Asset a = requireById(id);
        try {
            a.retire(req.reason());
        } catch (IllegalStateException ex) {
            throw new BusinessRuleViolationException("asset.retire_illegal_state", ex.getMessage());
        }
        return a;
    }

    /**
     * Closes the maintenance loop: UNDER_MAINTENANCE → IN_STOCK, so a repaired
     * asset can be assigned again.
     */
    @AuditWrite(entity = "Asset", action = "STATUS_CHANGE")
    public Asset completeMaintenance(Long id) {
        Asset a = requireById(id);
        try {
            a.markMaintenanceDone();
        } catch (IllegalStateException ex) {
            throw new BusinessRuleViolationException("asset.not_under_maintenance", ex.getMessage());
        }
        return a;
    }

    @Transactional(readOnly = true)
    public Asset findById(Long id) { return requireById(id); }

    @Transactional(readOnly = true)
    public Page<Asset> search(String q, AssetStatus status, Long categoryId, Pageable pageable) {
        return repo.search(q, status, categoryId, pageable);
    }

    Asset requireById(Long id) {
        return repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Asset", id));
    }
}
