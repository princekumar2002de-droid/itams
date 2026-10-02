package com.princekumar.itams.assignment;

import com.princekumar.itams.common.audit.AuditWrite;
import com.princekumar.itams.asset.Asset;
import com.princekumar.itams.asset.AssetRepository;
import com.princekumar.itams.asset.AssetStatus;
import com.princekumar.itams.assignment.dto.AssignAssetRequest;
import com.princekumar.itams.assignment.dto.ReturnAssetRequest;
import com.princekumar.itams.auth.CurrentUser;
import com.princekumar.itams.common.exception.BusinessRuleViolationException;
import com.princekumar.itams.common.exception.ResourceNotFoundException;
import com.princekumar.itams.person.Person;
import com.princekumar.itams.person.PersonRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AssetAssignmentService {

    private final AssetAssignmentRepository repo;
    private final AssetRepository assetRepo;
    private final PersonRepository personRepo;

    public AssetAssignmentService(AssetAssignmentRepository repo,
                                  AssetRepository assetRepo,
                                  PersonRepository personRepo) {
        this.repo = repo;
        this.assetRepo = assetRepo;
        this.personRepo = personRepo;
    }

    // ── assign ───────────────────────────────────────────────────────────────

    @AuditWrite(entity = "AssetAssignment", action = "ASSIGN")
    public AssetAssignment assign(AssignAssetRequest req) {
        long actingUserId = CurrentUser.id();  // set by JwtAuthenticationFilter

        // SELECT ... FOR UPDATE on the asset row — serialises concurrent assigns.
        Asset asset = assetRepo.findByIdForUpdate(req.assetId())
            .orElseThrow(() -> new ResourceNotFoundException("Asset", req.assetId()));

        Person assignee = personRepo.findByIdAndDeletedAtIsNull(req.assigneePersonId())
            .orElseThrow(() -> new ResourceNotFoundException("Person", req.assigneePersonId()));
        if (!assignee.isActive()) {
            throw new BusinessRuleViolationException(
                "assignment.assignee_inactive", "Assignee is not active.");
        }

        if (asset.getStatus() != AssetStatus.IN_STOCK) {
            throw new BusinessRuleViolationException(
                "assignment.asset_not_in_stock",
                "Asset is %s and cannot be assigned.".formatted(asset.getStatus()));
        }

        repo.findOpenByAssetId(asset.getId()).ifPresent(existing -> {
            throw new BusinessRuleViolationException(
                "assignment.already_open",
                "Asset already has an open assignment (id=%d).".formatted(existing.getId()));
        });

        AssetAssignment a = new AssetAssignment(
            asset, assignee, actingUserId,
            req.expectedReturnOn(), req.outCondition(), req.notes()
        );
        asset.markAssigned();
        return repo.save(a);
    }

    // ── return ───────────────────────────────────────────────────────────────

    @AuditWrite(entity = "AssetAssignment", action = "RETURN")
    public AssetAssignment returnAssignment(Long assignmentId, ReturnAssetRequest req) {
        long actingUserId = CurrentUser.id();

        AssetAssignment a = repo.findById(assignmentId)
            .orElseThrow(() -> new ResourceNotFoundException("AssetAssignment", assignmentId));
        if (!a.isOpen()) {
            throw new BusinessRuleViolationException(
                "assignment.already_returned", "Assignment was already returned.");
        }
        try {
            a.getAsset().markReturned(req.sendForMaintenance());
            a.closeReturn(actingUserId, req.inCondition(), req.notes());
        } catch (IllegalStateException ex) {
            throw new BusinessRuleViolationException("assignment.return_illegal_state", ex.getMessage());
        }
        return a;
    }

    // ── queries ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public AssetAssignment findById(Long id) {
        return repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("AssetAssignment", id));
    }

    @Transactional(readOnly = true)
    public Page<AssetAssignment> search(Long assetId, Long personId, boolean onlyOpen, Pageable pageable) {
        return repo.search(assetId, personId, onlyOpen, pageable);
    }
}
