package com.iotech.qualitrack.platform.equipment.domain.model.queries;

/**
 * Query to retrieve the maintenance history of an equipment located in an environment (US50, TS36).
 *
 * @param laboratoryId laboratory that owns the equipment
 * @param environmentId environment where the equipment is located
 * @param equipmentId equipment whose history is requested
 */
public record GetMaintenanceByEquipmentIdQuery(Long laboratoryId, Long environmentId, Long equipmentId) {
    public GetMaintenanceByEquipmentIdQuery {
        if (laboratoryId == null || laboratoryId <= 0) {
            throw new IllegalArgumentException("Laboratory id is required and must be greater than 0.");
        }
        if (environmentId == null || environmentId <= 0) {
            throw new IllegalArgumentException("Environment id is required and must be greater than 0.");
        }
        if (equipmentId == null || equipmentId <= 0) {
            throw new IllegalArgumentException("Equipment id is required and must be greater than 0.");
        }
    }
}
