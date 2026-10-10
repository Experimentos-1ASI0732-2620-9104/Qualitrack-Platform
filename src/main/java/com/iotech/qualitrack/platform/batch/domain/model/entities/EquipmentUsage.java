package com.iotech.qualitrack.platform.batch.domain.model.entities;

import lombok.Getter;

import java.util.Objects;

/**
 * Evidence that an equipment took part in the manufacturing of a product batch (US76).
 *
 * <p>The equipment name is copied when the usage is registered, so traceability keeps showing what
 * was used even if the equipment is renamed later.</p>
 */
@Getter
public class EquipmentUsage {
    private Long id;
    private Long batchId;
    private Long equipmentId;
    private String equipmentName;
    private Long registeredByUserId;
    private String registeredAt;

    /**
     * Reconstructs an equipment usage from persistence data.
     */
    public EquipmentUsage(Long id, Long batchId, Long equipmentId, String equipmentName, Long registeredByUserId,
                          String registeredAt) {
        this.id = id;
        this.batchId = batchId;
        this.equipmentId = equipmentId;
        this.equipmentName = equipmentName;
        this.registeredByUserId = registeredByUserId;
        this.registeredAt = registeredAt;
    }

    /**
     * Registers that the equipment was used in the batch.
     */
    public EquipmentUsage(Long batchId, Long equipmentId, String equipmentName, Long registeredByUserId, String registeredAt) {
        this(null, Objects.requireNonNull(batchId, "Batch ID is required"),
                Objects.requireNonNull(equipmentId, "Equipment ID is required"),
                Objects.requireNonNull(equipmentName, "Equipment name is required"),
                registeredByUserId, Objects.requireNonNull(registeredAt, "Registration time is required"));
    }
}
