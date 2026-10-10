package com.iotech.qualitrack.platform.inventory.interfaces.rest.transform;

import com.iotech.qualitrack.platform.inventory.domain.model.valueobjects.MaterialStockSummary;
import com.iotech.qualitrack.platform.inventory.interfaces.rest.resources.RawMaterialResource;
import com.iotech.qualitrack.platform.inventory.interfaces.rest.resources.RawMaterialStockResource;

public final class RawMaterialResourceFromEntityAssembler {
    private RawMaterialResourceFromEntityAssembler() { }

    public static RawMaterialResource toResourceFromEntity(MaterialStockSummary material) {
        return new RawMaterialResource(material.id(), material.laboratoryId(), material.environmentId(), material.code(),
            material.name(), material.unit(), material.minimumStock(), material.usableStock(), material.physicalStock(),
            material.stockStatus().name(), material.legacyId());
    }

    public static RawMaterialStockResource toStockResourceFromEntity(MaterialStockSummary material) {
        return new RawMaterialStockResource(material.id(), material.unit(), material.usableStock(), material.physicalStock(),
            material.minimumStock(), material.stockStatus().name());
    }
}
