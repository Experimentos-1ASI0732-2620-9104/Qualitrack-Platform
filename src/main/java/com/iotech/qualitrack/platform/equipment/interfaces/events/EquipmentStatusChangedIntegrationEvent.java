package com.iotech.qualitrack.platform.equipment.interfaces.events;

import com.iotech.qualitrack.platform.equipment.domain.model.events.EquipmentStatusChangedEvent;

/**
 * Integration event published when the operational status of an equipment changes.
 *
 * <p>This is part of the published language of the Equipment bounded context.</p>
 */
public record EquipmentStatusChangedIntegrationEvent(
        Long statusChangeId,
        Long equipmentId,
        Long laboratoryId,
        Long environmentId,
        String previousStatus,
        String newStatus,
        String reason,
        Long changedByUserId,
        String changedAt
) {
    public static EquipmentStatusChangedIntegrationEvent from(EquipmentStatusChangedEvent event) {
        return new EquipmentStatusChangedIntegrationEvent(event.statusChangeId(), event.equipmentId(),
                event.laboratoryId(), event.environmentId(), event.previousStatus().name(), event.newStatus().name(),
                event.reason(), event.changedByUserId(), event.changedAt().toString());
    }
}
