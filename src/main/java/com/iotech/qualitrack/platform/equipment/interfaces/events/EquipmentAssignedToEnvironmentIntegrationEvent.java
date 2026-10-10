package com.iotech.qualitrack.platform.equipment.interfaces.events;

import com.iotech.qualitrack.platform.equipment.domain.model.events.EquipmentAssignedToEnvironmentEvent;

/**
 * Integration event published when an equipment or IoT device is located in an environment.
 *
 * <p>This is part of the published language of the Equipment bounded context.</p>
 */
public record EquipmentAssignedToEnvironmentIntegrationEvent(
        Long equipmentId,
        Long laboratoryId,
        Long environmentId,
        Long previousEnvironmentId,
        String deviceType
) {
    public static EquipmentAssignedToEnvironmentIntegrationEvent from(EquipmentAssignedToEnvironmentEvent event) {
        return new EquipmentAssignedToEnvironmentIntegrationEvent(event.equipmentId(), event.laboratoryId(),
                event.environmentId(), event.previousEnvironmentId(),
                event.deviceType() == null ? null : event.deviceType().name());
    }
}
