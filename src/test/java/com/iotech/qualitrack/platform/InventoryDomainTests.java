package com.iotech.qualitrack.platform;

import com.iotech.qualitrack.platform.inventory.domain.model.aggregates.RawMaterial;
import com.iotech.qualitrack.platform.inventory.domain.model.aggregates.RawMaterialBatch;
import com.iotech.qualitrack.platform.inventory.domain.model.valueobjects.RawMaterialBatchStatus;
import com.iotech.qualitrack.platform.inventory.interfaces.acl.InventoryContextFacade.ConsumptionRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class InventoryDomainTests {
    private static final LocalDate RECEIVED = LocalDate.of(2026, 9, 1);
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 13);
    private static final LocalDate EXPIRY = LocalDate.of(2026, 10, 1);

    @Test
    void receiptRequiresReviewAndPreservesReceivedQuantityAfterConsumption() {
        var receipt = RawMaterialBatch.receive(1L, 10L, "Supplier", "LOT-A", "kilograms",
                new BigDecimal("100"), RECEIVED, EXPIRY);
        assertThat(receipt.getStatus()).isEqualTo(RawMaterialBatchStatus.QUARANTINED);
        assertThat(receipt.isUsableOn(TODAY)).isFalse();
        receipt.release();
        receipt.consume(new BigDecimal("50"), "kg", TODAY);
        assertThat(receipt.getAvailableAmount()).isEqualByComparingTo("50");
        assertThat(receipt.getInitialAmount()).isEqualByComparingTo("100");
        assertThat(receipt.getUnit()).isEqualTo("kg");
    }

    @Test
    void rejectsOverdraftWithoutChangingStock() {
        var receipt = receipt(1L, 1L, 10L, "kg", "100", EXPIRY, RawMaterialBatchStatus.RELEASED);
        assertThatThrownBy(() -> receipt.consume(new BigDecimal("200"), "kg", TODAY))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("Insufficient");
        assertThat(receipt.getAvailableAmount()).isEqualByComparingTo("100");
    }

    @ParameterizedTest
    @ValueSource(strings = {"g", "L", "mL", "units"})
    void rejectsDifferentUnitsWithoutConversion(String unit) {
        var receipt = receipt(1L, 1L, 10L, "kg", "100", EXPIRY, RawMaterialBatchStatus.RELEASED);
        assertThatThrownBy(() -> receipt.consume(BigDecimal.ONE, unit, TODAY)).isInstanceOf(IllegalArgumentException.class);
        assertThat(receipt.getAvailableAmount()).isEqualByComparingTo("100");
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "0.0001", "10000000000000000"})
    void rejectsInvalidAmountsWithoutMutation(String amount) {
        var receipt = receipt(1L, 1L, 10L, "kg", "100", EXPIRY, RawMaterialBatchStatus.RELEASED);
        assertThatThrownBy(() -> receipt.consume(new BigDecimal(amount), "kg", TODAY)).isInstanceOf(IllegalArgumentException.class);
        assertThat(receipt.getAvailableAmount()).isEqualByComparingTo("100");
    }

    @Test
    void decimalStockIsExactAndCountableUnitsMustBeWhole() {
        var receipt = receipt(1L, 1L, 10L, "L", "100.125", EXPIRY, RawMaterialBatchStatus.RELEASED);
        receipt.consume(new BigDecimal("0.125"), "litros", TODAY);
        assertThat(receipt.getAvailableAmount()).isEqualByComparingTo("100");
        var units = receipt(2L, 1L, 11L, "units", "100", EXPIRY, RawMaterialBatchStatus.RELEASED);
        assertThatThrownBy(() -> units.consume(new BigDecimal("0.5"), "units", TODAY)).isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @EnumSource(value = RawMaterialBatchStatus.class, names = {"QUARANTINED", "OBSERVED", "REJECTED"})
    void unusableStatesCannotBeConsumed(RawMaterialBatchStatus status) {
        var receipt = receipt(1L, 1L, 10L, "kg", "100", EXPIRY, status);
        assertThat(receipt.isUsableOn(TODAY)).isFalse();
        assertThatThrownBy(() -> receipt.consume(BigDecimal.ONE, "kg", TODAY)).isInstanceOf(IllegalStateException.class);
        assertThat(receipt.getAvailableAmount()).isEqualByComparingTo("100");
    }

    @Test
    void expiryDayAndDatesBeforeReceiptAreExcluded() {
        var receipt = receipt(1L, 1L, 10L, "kg", "100", EXPIRY, RawMaterialBatchStatus.RELEASED);
        assertThat(receipt.isUsableOn(RECEIVED)).isTrue();
        assertThat(receipt.isUsableOn(EXPIRY.minusDays(1))).isTrue();
        for (var date : List.of(RECEIVED.minusDays(1), EXPIRY, EXPIRY.plusDays(1))) {
            assertThatThrownBy(() -> receipt.consume(BigDecimal.ONE, "kg", date)).isInstanceOf(IllegalStateException.class);
        }
        assertThat(receipt.getAvailableAmount()).isEqualByComparingTo("100");
    }

    @Test
    void stockIsTheSumOfUsableReceiptsAndOnlyTheSelectedReceiptIsDecremented() {
        var a = receipt(1L, 1L, 10L, "kg", "100", EXPIRY, RawMaterialBatchStatus.RELEASED);
        var b = receipt(2L, 1L, 10L, "kg", "75.125", EXPIRY, RawMaterialBatchStatus.RELEASED);
        var blocked = receipt(3L, 1L, 10L, "kg", "50", EXPIRY, RawMaterialBatchStatus.OBSERVED);
        var expired = receipt(4L, 1L, 10L, "kg", "50", TODAY, RawMaterialBatchStatus.RELEASED);
        var receipts = List.of(a, b, blocked, expired);
        assertThat(material().usableStock(receipts, TODAY)).isEqualByComparingTo("175.125");
        a.consume(new BigDecimal("50"), "kg", TODAY);
        assertThat(material().usableStock(receipts, TODAY)).isEqualByComparingTo("125.125");
        assertThat(b.getAvailableAmount()).isEqualByComparingTo("75.125");
        a.consume(new BigDecimal("50"), "kg", TODAY);
        assertThat(a.isUsableOn(TODAY)).isFalse();
        assertThat(material().usableStock(List.of(), TODAY)).isZero();
    }

    @Test
    void stockCannotMixTenantsMaterialsUnitsOrDuplicateReceipts() {
        var a = receipt(1L, 1L, 10L, "kg", "100", EXPIRY, RawMaterialBatchStatus.RELEASED);
        var wrongTenant = receipt(2L, 2L, 10L, "kg", "100", EXPIRY, RawMaterialBatchStatus.RELEASED);
        var wrongMaterial = receipt(3L, 1L, 11L, "kg", "100", EXPIRY, RawMaterialBatchStatus.RELEASED);
        var wrongUnit = receipt(4L, 1L, 10L, "L", "100", EXPIRY, RawMaterialBatchStatus.RELEASED);
        for (var invalid : List.of(List.of(a, a), List.of(wrongTenant), List.of(wrongMaterial), List.of(wrongUnit))) {
            assertThatThrownBy(() -> material().usableStock(invalid, TODAY)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void observedAndRejectedReceiptsCannotBeReleasedWithoutTheRequiredReviewFlow() {
        var receipt = receipt(1L, 1L, 10L, "kg", "100", EXPIRY, RawMaterialBatchStatus.QUARANTINED);
        assertThatThrownBy(receipt::observe).isInstanceOf(IllegalStateException.class);
        receipt.release();
        assertThatThrownBy(receipt::release).isInstanceOf(IllegalStateException.class);
        receipt.observe();
        assertThatThrownBy(receipt::release).isInstanceOf(IllegalStateException.class);
        receipt.reject();
        assertThatThrownBy(receipt::release).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(receipt::reject).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void reconstructionRejectsImpossibleQuantitiesAndDates() {
        assertThatThrownBy(() -> new RawMaterialBatch(1L, 1L, 10L, "Supplier", "LOT-A", "kg",
                BigDecimal.ONE, BigDecimal.TEN, RECEIVED, EXPIRY, RawMaterialBatchStatus.RELEASED))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> receipt(1L, 1L, 10L, "kg", "100", RECEIVED.minusDays(1), RawMaterialBatchStatus.RELEASED))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void consumptionContractRequiresReceiptProductBatchAndAnOperationIdentity() {
        var request = new ConsumptionRequest(1L, 2L, 3L, BigDecimal.ONE, "kilograms", " operation-1 ");
        assertThat(request.unit()).isEqualTo("kg");
        assertThat(request.operationId()).isEqualTo("operation-1");
        assertThatThrownBy(() -> new ConsumptionRequest(1L, null, 3L, BigDecimal.ONE, "kg", "key"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ConsumptionRequest(1L, 2L, null, BigDecimal.ONE, "kg", "key"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ConsumptionRequest(1L, 2L, 3L, BigDecimal.ZERO, "kg", "key"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ConsumptionRequest(1L, 2L, 3L, BigDecimal.ONE, "kg", " "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private RawMaterial material() {
        return new RawMaterial(10L, 1L, 1L, "RM-10", "Material", "kg", BigDecimal.TEN);
    }

    private RawMaterialBatch receipt(Long id, Long laboratoryId, Long materialId, String unit,
                                     String amount, LocalDate expiresOn, RawMaterialBatchStatus status) {
        return new RawMaterialBatch(id, laboratoryId, materialId, "Supplier", "LOT-" + id,
                unit, new BigDecimal(amount), new BigDecimal(amount), RECEIVED, expiresOn, status);
    }
}
