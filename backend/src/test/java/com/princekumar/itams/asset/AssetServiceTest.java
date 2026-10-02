package com.princekumar.itams.asset;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for the Asset entity's state machine. The service simply
 * wraps {@code IllegalStateException} into a business-rule violation
 * (covered by the integration test).
 */
class AssetServiceTest {

    @Test
    void assign_return_retire_transitions() {
        Asset a = newAsset();
        assertThat(a.getStatus()).isEqualTo(AssetStatus.IN_STOCK);

        a.markAssigned();
        assertThat(a.getStatus()).isEqualTo(AssetStatus.ASSIGNED);

        a.markReturned(false);
        assertThat(a.getStatus()).isEqualTo(AssetStatus.IN_STOCK);

        a.retire("End of life");
        assertThat(a.getStatus()).isEqualTo(AssetStatus.RETIRED);
        assertThat(a.getRetiredAt()).isNotNull();
        assertThat(a.getRetiredReason()).isEqualTo("End of life");
    }

    @Test
    void return_with_maintenance_flag_transitions_to_under_maintenance() {
        Asset a = newAsset();
        a.markAssigned();
        a.markReturned(true);
        assertThat(a.getStatus()).isEqualTo(AssetStatus.UNDER_MAINTENANCE);

        a.markMaintenanceDone();
        assertThat(a.getStatus()).isEqualTo(AssetStatus.IN_STOCK);
    }

    @Test
    void cannot_complete_maintenance_unless_under_maintenance() {
        Asset a = newAsset();   // IN_STOCK
        assertThatThrownBy(a::markMaintenanceDone).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void cannot_assign_when_not_in_stock() {
        Asset a = newAsset();
        a.markAssigned();
        assertThatThrownBy(a::markAssigned).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void cannot_return_when_not_assigned() {
        Asset a = newAsset();
        assertThatThrownBy(() -> a.markReturned(false)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void cannot_retire_when_assigned() {
        Asset a = newAsset();
        a.markAssigned();
        assertThatThrownBy(() -> a.retire("nope")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void cannot_retire_twice() {
        Asset a = newAsset();
        a.retire("first");
        assertThatThrownBy(() -> a.retire("again")).isInstanceOf(IllegalStateException.class);
    }

    private static Asset newAsset() {
        return new Asset(
            "A-0001", null, "SN-1",
            LocalDate.of(2026, 1, 1),
            new BigDecimal("1000.00"),
            LocalDate.of(2029, 1, 1),
            null);
    }
}
