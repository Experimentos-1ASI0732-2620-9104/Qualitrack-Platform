package com.iotech.qualitrack.platform.inventory.infrastructure.persistence.jpa.assemblers;
import com.iotech.qualitrack.platform.inventory.domain.model.aggregates.RawMaterial;
import com.iotech.qualitrack.platform.inventory.infrastructure.persistence.jpa.entities.InventoryMaterialEntity;

public final class InventoryMaterialPersistenceAssembler {
    private InventoryMaterialPersistenceAssembler() { }
    public static RawMaterial toDomainFromPersistence(InventoryMaterialEntity entity) {
        return new RawMaterial(entity.getId(), entity.getLaboratoryId(), entity.getEnvironmentId(), entity.getCode(),
            entity.getName(), entity.getUnit(), entity.getMinimumStock());
    }
    public static InventoryMaterialEntity toPersistenceFromDomain(RawMaterial material, Long legacyId) {
        var entity = new InventoryMaterialEntity();
        entity.setId(material.getId());
        entity.setLaboratoryId(material.getLaboratoryId());
        entity.setEnvironmentId(material.getEnvironmentId());
        entity.setCode(material.getCode());
        entity.setName(material.getName());
        entity.setUnit(material.getUnit());
        entity.setMinimumStock(material.getMinimumStock());
        entity.setLegacyId(legacyId);
        return entity;
    }
}
