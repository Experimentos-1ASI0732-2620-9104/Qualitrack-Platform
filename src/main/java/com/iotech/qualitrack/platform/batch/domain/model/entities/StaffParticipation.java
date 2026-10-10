package com.iotech.qualitrack.platform.batch.domain.model.entities;

import lombok.Getter;

import java.util.Objects;

/**
 * Evidence that a staff member took part in the manufacturing of a product batch (US77).
 *
 * <p>The name and role are copied when the participation is registered, so traceability keeps
 * showing who took part even if the staff record changes later.</p>
 */
@Getter
public class StaffParticipation {
    private Long id;
    private Long batchId;
    private Long staffId;
    private String staffName;
    private String staffRole;
    private Long registeredByUserId;
    private String registeredAt;

    /**
     * Reconstructs a staff participation from persistence data.
     */
    public StaffParticipation(Long id, Long batchId, Long staffId, String staffName, String staffRole,
                              Long registeredByUserId, String registeredAt) {
        this.id = id;
        this.batchId = batchId;
        this.staffId = staffId;
        this.staffName = staffName;
        this.staffRole = staffRole;
        this.registeredByUserId = registeredByUserId;
        this.registeredAt = registeredAt;
    }

    /**
     * Registers that the staff member took part in the batch.
     */
    public StaffParticipation(Long batchId, Long staffId, String staffName, String staffRole, Long registeredByUserId,
                              String registeredAt) {
        this(null, Objects.requireNonNull(batchId, "Batch ID is required"),
                Objects.requireNonNull(staffId, "Staff ID is required"),
                Objects.requireNonNull(staffName, "Staff name is required"), staffRole,
                registeredByUserId, Objects.requireNonNull(registeredAt, "Registration time is required"));
    }
}
