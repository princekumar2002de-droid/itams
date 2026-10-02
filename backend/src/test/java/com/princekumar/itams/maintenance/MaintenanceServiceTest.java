package com.princekumar.itams.maintenance;

import com.princekumar.itams.asset.Asset;
import com.princekumar.itams.asset.AssetRepository;
import com.princekumar.itams.common.exception.BusinessRuleViolationException;
import com.princekumar.itams.common.exception.ResourceNotFoundException;
import com.princekumar.itams.maintenance.dto.CreateMaintenanceRequest;
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
 * Fast unit tests for {@link MaintenanceService}. Covers:
 *  - "performer_required" rule: internal (userId) OR external (providerName) must be set;
 *  - asset lookup 404;
 *  - happy-path create.
 */
@ExtendWith(MockitoExtension.class)
class MaintenanceServiceTest {

    @Mock MaintenanceRecordRepository repo;
    @Mock AssetRepository assetRepo;
    @InjectMocks MaintenanceService service;

    @Test
    void create_rejects_when_neither_performer_given() {
        var req = new CreateMaintenanceRequest(
            1L, LocalDate.now(),
            null, null,
            "Cleaned fans", null, null);

        assertThatThrownBy(() -> service.create(req))
            .isInstanceOf(BusinessRuleViolationException.class)
            .hasMessageContaining("performedByUserId");
        verify(assetRepo, never()).findById(any());
        verify(repo, never()).save(any());
    }

    @Test
    void create_accepts_internal_performer() {
        var req = new CreateMaintenanceRequest(
            5L, LocalDate.now(),
            42L, null,
            "Reseated RAM", new BigDecimal("0.00"), null);
        Asset asset = new Asset("A-1", null, "SN", LocalDate.now(), new BigDecimal("100"), null, null);
        when(assetRepo.findById(5L)).thenReturn(Optional.of(asset));
        when(repo.save(any(MaintenanceRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        MaintenanceRecord result = service.create(req);

        assertThat(result.getPerformedByUserId()).isEqualTo(42L);
        assertThat(result.getProviderName()).isNull();
        assertThat(result.getAsset()).isSameAs(asset);
    }

    @Test
    void create_accepts_external_provider() {
        var req = new CreateMaintenanceRequest(
            5L, LocalDate.now(),
            null, "ACME Repairs GmbH",
            "Screen replaced", new BigDecimal("199.00"), LocalDate.now().plusYears(1));
        Asset asset = new Asset("A-1", null, "SN", LocalDate.now(), new BigDecimal("100"), null, null);
        when(assetRepo.findById(5L)).thenReturn(Optional.of(asset));
        when(repo.save(any(MaintenanceRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        MaintenanceRecord result = service.create(req);

        assertThat(result.getProviderName()).isEqualTo("ACME Repairs GmbH");
        assertThat(result.getPerformedByUserId()).isNull();
    }

    @Test
    void create_blank_provider_still_treated_as_missing() {
        var req = new CreateMaintenanceRequest(
            1L, LocalDate.now(),
            null, "   ",
            "no-op", null, null);
        assertThatThrownBy(() -> service.create(req))
            .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void create_with_missing_asset_throws_not_found() {
        var req = new CreateMaintenanceRequest(
            999L, LocalDate.now(),
            42L, null,
            "n/a", null, null);
        when(assetRepo.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(req))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("999");
    }
}
