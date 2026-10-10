package com.iotech.qualitrack.platform.batch.infrastructure.persistence.jpa.assemblers;

import com.iotech.qualitrack.platform.batch.domain.model.entities.RawMaterialUsage;
import com.iotech.qualitrack.platform.batch.infrastructure.persistence.jpa.entities.RawMaterialUsagePersistenceEntity;

/**
 * Static assembler between raw material usage domain and persistence representations.
 */
public final class RawMaterialUsagePersistenceAssembler {

    private RawMaterialUsagePersistenceAssembler() {
    }

    public static RawMaterialUsage toDomainFromPersistence(RawMaterialUsagePersistenceEntity entity) {
        if (entity == null) return null;

        var usage = new RawMaterialUsage(
                entity.getId(),
                entity.getBatchId(),
                entity.getRawMaterialId(),
                entity.getRawMaterialName(),
                entity.getQuantityUsed(),
                entity.getUnit(),
                entity.getUsageDate()
        );
        usage.recordStockChange(entity.getStockBefore(), entity.getStockAfter());
        usage.assignInventoryReceipt(entity.getInventoryReceiptId());
        usage.assignOperation(entity.getOperationId());
        return usage;
    }

    public static RawMaterialUsagePersistenceEntity toPersistenceFromDomain(RawMaterialUsage usage) {
        if (usage == null) return null;

        var entity = new RawMaterialUsagePersistenceEntity();

        if (usage.getId() != null) {
            entity.setId(usage.getId());
        }

        entity.setBatchId(usage.getBatchId());
        entity.setRawMaterialId(usage.getRawMaterialId());
        entity.setRawMaterialName(usage.getRawMaterialName());
        entity.setQuantityUsed(usage.getQuantityUsed());
        entity.setUnit(usage.getUnit());
        entity.setUsageDate(usage.getUsageDate());
        entity.setStockBefore(usage.getStockBefore());
        entity.setStockAfter(usage.getStockAfter());
        entity.setInventoryReceiptId(usage.getInventoryReceiptId());
        entity.setOperationId(usage.getOperationId());

        return entity;
    }
}
