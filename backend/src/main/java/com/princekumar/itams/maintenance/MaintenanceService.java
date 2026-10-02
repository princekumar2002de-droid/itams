package com.princekumar.itams.maintenance;

import com.princekumar.itams.common.audit.AuditWrite;
import com.princekumar.itams.asset.Asset;
import com.princekumar.itams.asset.AssetRepository;
import com.princekumar.itams.common.exception.BusinessRuleViolationException;
import com.princekumar.itams.common.exception.ResourceNotFoundException;
import com.princekumar.itams.maintenance.dto.CreateMaintenanceRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class MaintenanceService {

    private final MaintenanceRecordRepository repo;
    private final AssetRepository assetRepo;

    public MaintenanceService(MaintenanceRecordRepository repo, AssetRepository assetRepo) {
        this.repo = repo;
        this.assetRepo = assetRepo;
    }

    @AuditWrite(entity = "MaintenanceRecord", action = "CREATE")
    public MaintenanceRecord create(CreateMaintenanceRequest req) {
        if (req.performedByUserId() == null && (req.providerName() == null || req.providerName().isBlank())) {
            throw new BusinessRuleViolationException(
                "maintenance.performer_required",
                "Must specify either performedByUserId (internal) or providerName (external).");
        }
        Asset asset = assetRepo.findById(req.assetId())
            .orElseThrow(() -> new ResourceNotFoundException("Asset", req.assetId()));

        return repo.save(new MaintenanceRecord(
            asset, req.performedOn(),
            req.performedByUserId(), req.providerName(),
            req.description(), req.cost(), req.nextScheduledOn()
        ));
    }

    @Transactional(readOnly = true)
    public MaintenanceRecord findById(Long id) {
        return repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("MaintenanceRecord", id));
    }

    @Transactional(readOnly = true)
    public Page<MaintenanceRecord> search(String q, Long assetId, Pageable pageable) {
        return repo.search(q, assetId, pageable);
    }

    @Transactional(readOnly = true)
    public List<MaintenanceRecord> byAsset(Long assetId) {
        return repo.findByAssetId(assetId);
    }

    @AuditWrite(entity = "MaintenanceRecord", action = "DELETE")
    public void delete(Long id) {
        repo.delete(findById(id));
    }
}
