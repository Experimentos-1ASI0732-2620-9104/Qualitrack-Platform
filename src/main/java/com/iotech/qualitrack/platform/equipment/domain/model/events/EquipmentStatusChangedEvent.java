package com.iotech.qualitrack.platform.equipment.domain.model.events;

import com.iotech.qualitrack.platform.equipment.domain.model.aggregates.Equipment;
import com.iotech.qualitrack.platform.equipment.domain.model.entities.EquipmentStatusChange;
import com.iotech.qualitrack.platform.equipment.domain.model.valueobjects.EquipmentStatus;

import java.time.Instant;

/**
 * Domain event published when the operational status of an equipment changes (US48).
 */
public record EquipmentStatusChangedEvent(
        Long statusChangeId,
        Long equipmentId,
        Long laboratoryId,
        Long environmentId,
        EquipmentStatus previousStatus,
        EquipmentStatus newStatus,
        String reason,
        Long changedByUserId,
        Instant changedAt
) {
    public static EquipmentStatusChangedEvent from(Equipment equipment, EquipmentStatusChange change) {
        return new EquipmentStatusChangedEvent(change.getId(), equipment.getId(), equipment.getLabId(),
                change.getEnvironmentId(), change.getPreviousStatus(), change.getNewStatus(), change.getReason(),
                change.getChangedByUserId(), change.getChangedAt());
    }
}
