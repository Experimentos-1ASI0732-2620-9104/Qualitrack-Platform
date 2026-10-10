package com.iotech.qualitrack.platform.equipment.domain.model.queries;

/**
 * Query to retrieve an equipment of a laboratory by its numeric identifier.
 *
 * @param laboratoryId laboratory that must own the equipment
 * @param equipmentId equipment identifier
 */
public record GetEquipmentByIdQuery(Long laboratoryId, Long equipmentId) {
    public GetEquipmentByIdQuery {
        if (laboratoryId == null || laboratoryId <= 0) {
            throw new IllegalArgumentException("Laboratory id is required and must be greater than 0.");
        }
        if (equipmentId == null || equipmentId <= 0) {
            throw new IllegalArgumentException("Equipment id is required and must be greater than 0.");
        }
    }
}
