package com.iotech.qualitrack.platform.equipment.application.queryservices;

import com.iotech.qualitrack.platform.equipment.domain.model.aggregates.MaintenanceRecord;
import com.iotech.qualitrack.platform.equipment.domain.model.queries.GetMaintenanceByEquipmentIdQuery;

import java.util.List;
import java.util.Optional;

/**
 * Application service contract for maintenance record read queries.
 */
public interface MaintenanceQueryService {

    /**
     * Handles retrieval of the maintenance history for a specific equipment.
     *
     * @param query laboratory, environment and equipment of the history
     * @return maintenance records of the equipment, newest first, or empty when it is not located in the environment
     * @see GetMaintenanceByEquipmentIdQuery
     */
    Optional<List<MaintenanceRecord>> handle(GetMaintenanceByEquipmentIdQuery query);
}