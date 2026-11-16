package com.iotech.qualitrack.platform.inventory.infrastructure.persistence.jpa.assemblers;
import com.iotech.qualitrack.platform.inventory.domain.model.aggregates.RawMaterial;
import com.iotech.qualitrack.platform.inventory.infrastructure.persistence.jpa.entities.InventoryMaterialPersistenceEntity;

/**
 * Maps raw material aggregates to and from JPA persistence entities.
 *
 * <p>Keeps persistence details isolated from the Inventory domain
 * while preserving material identifiers, laboratory ownership,
 * environment assignment and minimum stock configuration.</p>
 */
public final class InventoryMaterialPersistenceAssembler {

    private InventoryMaterialPersistenceAssembler() { }

    /**
     * Reconstructs a raw material aggregate from its persistence entity.
     *
     * @param entity persisted raw material
     * @return reconstructed domain aggregate
     */
    public static RawMaterial toDomainFromPersistence(InventoryMaterialPersistenceEntity entity) {
        return new RawMaterial(entity.getId(), entity.getLaboratoryId(), entity.getEnvironmentId(), entity.getCode(),
            entity.getName(), entity.getUnit(), entity.getMinimumStock());
    }
    /**
     * Converts a raw material aggregate into a JPA entity.
     *
     * <p>The optional legacy identifier is stored only as a
     * persistence reference and is not part of the aggregate.</p>
     *
     * @param material raw material aggregate
     * @param legacyId optional legacy material identifier
     * @return persistence entity containing the material data
     */
    public static InventoryMaterialPersistenceEntity toPersistenceFromDomain(RawMaterial material, Long legacyId) {
        var entity = new InventoryMaterialPersistenceEntity();
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
