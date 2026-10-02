package com.princekumar.itams.license;

import com.princekumar.itams.common.audit.AuditWrite;
import com.princekumar.itams.asset.Asset;
import com.princekumar.itams.asset.AssetRepository;
import com.princekumar.itams.common.exception.BusinessRuleViolationException;
import com.princekumar.itams.common.exception.ResourceNotFoundException;
import com.princekumar.itams.license.dto.AssignLicenseRequest;
import com.princekumar.itams.license.dto.CreateLicenseRequest;
import com.princekumar.itams.person.Person;
import com.princekumar.itams.person.PersonRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class SoftwareLicenseService {

    private final SoftwareLicenseRepository licenseRepo;
    private final SoftwareProductRepository productRepo;
    private final LicenseAssignmentRepository assignmentRepo;
    private final PersonRepository personRepo;
    private final AssetRepository assetRepo;

    public SoftwareLicenseService(SoftwareLicenseRepository licenseRepo,
                                  SoftwareProductRepository productRepo,
                                  LicenseAssignmentRepository assignmentRepo,
                                  PersonRepository personRepo,
                                  AssetRepository assetRepo) {
        this.licenseRepo = licenseRepo;
        this.productRepo = productRepo;
        this.assignmentRepo = assignmentRepo;
        this.personRepo = personRepo;
        this.assetRepo = assetRepo;
    }

    // ── licenses ─────────────────────────────────────────────────────────────

    @AuditWrite(entity = "SoftwareLicense", action = "CREATE")
    public SoftwareLicense create(CreateLicenseRequest req) {
        SoftwareProduct product = productRepo
            .findByNaturalKey(req.vendor(), req.productName(), req.productVersion())
            .orElseGet(() -> productRepo.save(
                new SoftwareProduct(req.vendor(), req.productName(), req.productVersion())));

        SoftwareLicense license = new SoftwareLicense(
            product, req.licenseReference(), req.licenseType(),
            req.seatsTotal(), req.purchaseDate(), req.expiresOn(),
            req.cost(), req.procurementRef()
        );
        return licenseRepo.save(license);
    }

    @Transactional(readOnly = true)
    public SoftwareLicense findById(Long id) {
        return licenseRepo.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("SoftwareLicense", id));
    }

    @Transactional(readOnly = true)
    public Page<SoftwareLicense> search(String q, Pageable pageable) {
        return licenseRepo.search(q, pageable);
    }

    @Transactional(readOnly = true)
    public long countOpen(Long licenseId) {
        return assignmentRepo.countOpenByLicenseId(licenseId);
    }

    // ── assignments ──────────────────────────────────────────────────────────

    @AuditWrite(entity = "LicenseAssignment", action = "ASSIGN")
    public LicenseAssignment assign(Long licenseId, AssignLicenseRequest req) {
        if ((req.personId() == null) == (req.assetId() == null)) {
            throw new BusinessRuleViolationException(
                "license.assignee_xor", "Assign to a person OR an asset — not both, not neither.");
        }

        // Lock the license row so concurrent assign attempts can't both see the same seatsUsed.
        SoftwareLicense license = licenseRepo.findByIdForUpdate(licenseId)
            .orElseThrow(() -> new ResourceNotFoundException("SoftwareLicense", licenseId));

        long used = assignmentRepo.countOpenByLicenseId(licenseId);
        if (used >= license.getSeatsTotal()) {
            throw new BusinessRuleViolationException(
                "license.out_of_seats",
                "License has %d/%d seats in use.".formatted(used, license.getSeatsTotal()));
        }

        Person person = null;
        Asset asset = null;
        if (req.personId() != null) {
            person = personRepo.findByIdAndDeletedAtIsNull(req.personId())
                .orElseThrow(() -> new ResourceNotFoundException("Person", req.personId()));
        } else {
            asset = assetRepo.findById(req.assetId())
                .orElseThrow(() -> new ResourceNotFoundException("Asset", req.assetId()));
        }

        return assignmentRepo.save(new LicenseAssignment(license, person, asset, req.notes()));
    }

    @AuditWrite(entity = "LicenseAssignment", action = "RETURN")
    public LicenseAssignment release(Long assignmentId) {
        LicenseAssignment a = assignmentRepo.findById(assignmentId)
            .orElseThrow(() -> new ResourceNotFoundException("LicenseAssignment", assignmentId));
        if (!a.isOpen()) {
            throw new BusinessRuleViolationException(
                "license.already_released", "That seat was already released.");
        }
        a.release();
        return a;
    }

    @Transactional(readOnly = true)
    public List<LicenseAssignment> assignmentsFor(Long licenseId) {
        return assignmentRepo.findByLicenseId(licenseId);
    }
}
