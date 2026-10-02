package com.princekumar.itams.license;

import com.princekumar.itams.asset.Asset;
import com.princekumar.itams.asset.AssetRepository;
import com.princekumar.itams.common.exception.BusinessRuleViolationException;
import com.princekumar.itams.common.exception.ResourceNotFoundException;
import com.princekumar.itams.license.dto.AssignLicenseRequest;
import com.princekumar.itams.person.Person;
import com.princekumar.itams.person.PersonRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Fast unit tests for {@link SoftwareLicenseService}. Covers the two business
 * rules the service enforces on top of JPA:
 *   1) an assignment goes to a person XOR an asset, never both, never neither;
 *   2) you can't allocate more seats than the license has.
 */
@ExtendWith(MockitoExtension.class)
class SoftwareLicenseServiceTest {

    @Mock SoftwareLicenseRepository licenseRepo;
    @Mock SoftwareProductRepository productRepo;
    @Mock LicenseAssignmentRepository assignmentRepo;
    @Mock PersonRepository personRepo;
    @Mock AssetRepository assetRepo;
    @InjectMocks SoftwareLicenseService service;

    @Test
    void assign_rejects_when_neither_person_nor_asset_given() {
        var req = new AssignLicenseRequest(null, null, null);
        assertThatThrownBy(() -> service.assign(1L, req))
            .isInstanceOf(BusinessRuleViolationException.class)
            .hasMessageContaining("person OR an asset");
        verify(licenseRepo, never()).findByIdForUpdate(any());
    }

    @Test
    void assign_rejects_when_both_person_and_asset_given() {
        var req = new AssignLicenseRequest(1L, 1L, null);
        assertThatThrownBy(() -> service.assign(1L, req))
            .isInstanceOf(BusinessRuleViolationException.class)
            .hasMessageContaining("person OR an asset");
    }

    @Test
    void assign_rejects_when_all_seats_are_used() {
        SoftwareLicense license = license(2);
        when(licenseRepo.findByIdForUpdate(10L)).thenReturn(Optional.of(license));
        when(assignmentRepo.countOpenByLicenseId(10L)).thenReturn(2L);

        var req = new AssignLicenseRequest(1L, null, null);
        assertThatThrownBy(() -> service.assign(10L, req))
            .isInstanceOf(BusinessRuleViolationException.class)
            .hasMessageContaining("2/2 seats");
        verify(assignmentRepo, never()).save(any());
    }

    @Test
    void assign_to_person_persists_when_seats_available() {
        SoftwareLicense license = license(5);
        Person p = new Person("Alex", "K", "alex@example.com", null);
        when(licenseRepo.findByIdForUpdate(10L)).thenReturn(Optional.of(license));
        when(assignmentRepo.countOpenByLicenseId(10L)).thenReturn(1L);
        when(personRepo.findByIdAndDeletedAtIsNull(7L)).thenReturn(Optional.of(p));
        when(assignmentRepo.save(any(LicenseAssignment.class))).thenAnswer(inv -> inv.getArgument(0));

        LicenseAssignment created = service.assign(10L, new AssignLicenseRequest(7L, null, "onboarding"));

        assertThat(created.getPerson()).isSameAs(p);
        assertThat(created.getAsset()).isNull();
        assertThat(created.isOpen()).isTrue();
    }

    @Test
    void assign_to_missing_person_throws_not_found() {
        SoftwareLicense license = license(5);
        when(licenseRepo.findByIdForUpdate(10L)).thenReturn(Optional.of(license));
        when(assignmentRepo.countOpenByLicenseId(10L)).thenReturn(0L);
        when(personRepo.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.assign(10L, new AssignLicenseRequest(999L, null, null)))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("999");
    }

    @Test
    void release_rejects_double_release() {
        Person p = new Person("A", "B", "a@b.c", null);
        LicenseAssignment a = new LicenseAssignment(license(5), p, null, null);
        a.release();
        when(assignmentRepo.findById(3L)).thenReturn(Optional.of(a));

        assertThatThrownBy(() -> service.release(3L))
            .isInstanceOf(BusinessRuleViolationException.class)
            .hasMessageContaining("already released");
    }

    @Test
    void release_marks_open_assignment_closed() {
        Person p = new Person("A", "B", "a@b.c", null);
        LicenseAssignment a = new LicenseAssignment(license(5), p, null, null);
        when(assignmentRepo.findById(3L)).thenReturn(Optional.of(a));

        LicenseAssignment result = service.release(3L);
        assertThat(result.isOpen()).isFalse();
        assertThat(result.getReleasedAt()).isNotNull();
    }

    @Test
    void assign_to_asset_only_persists_that_side() {
        SoftwareLicense license = license(5);
        Asset asset = new Asset("A-1", null, "SN", LocalDate.now(), new BigDecimal("100"), null, null);
        when(licenseRepo.findByIdForUpdate(10L)).thenReturn(Optional.of(license));
        when(assignmentRepo.countOpenByLicenseId(10L)).thenReturn(0L);
        when(assetRepo.findById(50L)).thenReturn(Optional.of(asset));
        when(assignmentRepo.save(any(LicenseAssignment.class))).thenAnswer(inv -> inv.getArgument(0));

        LicenseAssignment created = service.assign(10L, new AssignLicenseRequest(null, 50L, null));
        assertThat(created.getAsset()).isSameAs(asset);
        assertThat(created.getPerson()).isNull();
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private static SoftwareLicense license(int seats) {
        SoftwareProduct product = new SoftwareProduct("Vendor", "Product", "1.0");
        return new SoftwareLicense(product, "REF-1", LicenseType.PER_SEAT,
            seats, LocalDate.now().minusMonths(1), LocalDate.now().plusYears(1),
            new BigDecimal("100.00"), null);
    }
}
