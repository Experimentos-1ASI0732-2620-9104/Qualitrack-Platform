package com.iotech.qualitrack.platform.equipment.domain.model.events;

import com.iotech.qualitrack.platform.equipment.domain.model.aggregates.Equipment;
import com.iotech.qualitrack.platform.equipment.domain.model.valueobjects.IotDeviceType;

/**
 * Domain event published when an equipment or IoT device is located in an environment (US47, US52, US54).
 *
 * @param previousEnvironmentId environment where it was located before, or null
 * @param deviceType IoT device type, or null for ordinary equipment
 */
public record EquipmentAssignedToEnvironmentEvent(
        Long equipmentId,
        Long laboratoryId,
        Long environmentId,
        Long previousEnvironmentId,
        IotDeviceType deviceType
) {
    public static EquipmentAssignedToEnvironmentEvent from(Equipment equipment, Long previousEnvironmentId) {
        return new EquipmentAssignedToEnvironmentEvent(equipment.getId(), equipment.getLabId(),
                equipment.getEnvironmentId(), previousEnvironmentId, equipment.getDeviceType());
    }
}
