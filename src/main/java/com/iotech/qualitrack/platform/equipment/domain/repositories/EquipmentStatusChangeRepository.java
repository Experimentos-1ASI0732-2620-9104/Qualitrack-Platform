package com.iotech.qualitrack.platform.equipment.domain.repositories;

import com.iotech.qualitrack.platform.equipment.domain.model.entities.EquipmentStatusChange;

/**
 * Keeps the history of operational status changes of equipment (US48).
 */
public interface EquipmentStatusChangeRepository {

    EquipmentStatusChange save(EquipmentStatusChange statusChange);
}
