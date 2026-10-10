package com.iotech.qualitrack.platform.equipment.interfaces.rest.transform;

import com.iotech.qualitrack.platform.equipment.domain.model.aggregates.MaintenanceRecord;
import com.iotech.qualitrack.platform.equipment.interfaces.rest.resources.MaintenanceRecordResource;

/**
 * Assembler to convert a MaintenanceRecord entity to a MaintenanceRecordResource.
 */
public class MaintenanceResourceFromEntityAssembler {

    /**
     * Converts a MaintenanceRecord entity to a MaintenanceRecordResource.
     *
     * @param entity The {@link MaintenanceRecord} entity to convert.
     * @return The {@link MaintenanceRecordResource} resource that results from the conversion.
     */
    public static MaintenanceRecordResource toResourceFromEntity(MaintenanceRecord entity) {
        return new MaintenanceRecordResource(
                entity.getId(),
                entity.getEquipmentId(),
                entity.getEnvironmentId(),
                entity.getMaintenanceDate() != null ? entity.getMaintenanceDate().toString() : null,
                entity.getTechnicianName(),
                entity.getTechnicianStaffId(),
                entity.getDescription(),
                entity.getType().name()
        );
    }
}