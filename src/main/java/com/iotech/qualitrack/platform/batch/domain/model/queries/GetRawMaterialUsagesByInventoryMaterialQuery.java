package com.iotech.qualitrack.platform.batch.domain.model.queries;

/**
 * Product batch usages of an Inventory raw material, recorded when its lots were consumed (TS79, US89).
 *
 * @param rawMaterialId Inventory raw material identifier
 */
public record GetRawMaterialUsagesByInventoryMaterialQuery(Long rawMaterialId) {
    public GetRawMaterialUsagesByInventoryMaterialQuery {
        if (rawMaterialId == null || rawMaterialId <= 0) throw new IllegalArgumentException("Invalid raw material ID");
    }
}
