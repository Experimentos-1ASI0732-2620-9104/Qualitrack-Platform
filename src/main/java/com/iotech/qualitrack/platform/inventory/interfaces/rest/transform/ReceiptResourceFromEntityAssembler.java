package com.iotech.qualitrack.platform.inventory.interfaces.rest.transform;

import com.iotech.qualitrack.platform.inventory.domain.model.aggregates.RawMaterialBatch;
import com.iotech.qualitrack.platform.inventory.domain.model.valueobjects.NearExpiryPeriod;
import com.iotech.qualitrack.platform.inventory.interfaces.rest.resources.ReceiptResource;

import java.time.LocalDate;

public final class ReceiptResourceFromEntityAssembler {
    private ReceiptResourceFromEntityAssembler() { }

    public static ReceiptResource toResourceFromEntity(RawMaterialBatch receipt, LocalDate today, NearExpiryPeriod nearExpiry) {
        String availability = !today.isBefore(receipt.getExpiresOn()) ? "EXPIRED"
            : today.isBefore(receipt.getReceivedOn()) ? "NOT_YET_RECEIVED"
            : receipt.getAvailableAmount().signum() == 0 ? "DEPLETED" : receipt.getStatus().name();
        return new ReceiptResource(receipt.getId(), receipt.getLaboratoryId(), receipt.getRawMaterialId(),
            receipt.getSupplier(), receipt.getBatchNumber(), receipt.getUnit(), receipt.getInitialAmount(),
            receipt.getAvailableAmount(), receipt.getReceivedOn(), receipt.getExpiresOn(), receipt.getStatus().name(),
            receipt.isUsableOn(today), availability, receipt.expirationStatus(today, nearExpiry.days()).name(),
            receipt.container().map(container -> container.containerMonitorId()).orElse(null));
    }
}
