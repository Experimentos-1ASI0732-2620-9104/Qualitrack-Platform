package com.iotech.qualitrack.platform.equipment.domain.model.entities;

import com.iotech.qualitrack.platform.equipment.domain.model.valueobjects.EquipmentStatus;
import lombok.Getter;

import java.time.Instant;

/**
 * Traceable record of a change in the operational status of an equipment (US48, TS34).
 */
@Getter
public class EquipmentStatusChange {
    private final Long id;
    private final Long equipmentId;
    private final Long environmentId;
    private final EquipmentStatus previousStatus;
    private final EquipmentStatus newStatus;
    private final String reason;
    private final Long changedByUserId;
    private final Instant changedAt;

    public EquipmentStatusChange(Long id, Long equipmentId, Long environmentId, EquipmentStatus previousStatus,
                                 EquipmentStatus newStatus, String reason, Long changedByUserId, Instant changedAt) {
        this.id = id;
        this.equipmentId = equipmentId;
        this.environmentId = environmentId;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.reason = reason;
        this.changedByUserId = changedByUserId;
        this.changedAt = changedAt;
    }
}
