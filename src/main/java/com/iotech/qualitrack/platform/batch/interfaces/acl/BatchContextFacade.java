package com.iotech.qualitrack.platform.batch.interfaces.acl;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface BatchContextFacade {
    void requireConsumable(Long batchId, Long laboratoryId);

    boolean existsBatchById(Long batchId);

    boolean belongsToLaboratory(Long batchId, Long laboratoryId);

    boolean isBatchReleased(Long batchId);

    boolean isBatchRejected(Long batchId);

    /**
     * Resources that took part in a batch (US80, US96): consumed lots, equipment, staff, container and the quality
     * decision.
     *
     * @param batchId the product batch
     * @return the traceability, or empty when the batch does not exist or has no environment (older batches)
     */
    Optional<TraceabilityReference> findTraceability(Long batchId);

    /**
     * Traceability of a product batch shared with other bounded contexts.
     *
     * @param container the container where the batch is stored, or null
     * @param release the release signature, or null
     * @param rejection the rejection record, or null
     */
    record TraceabilityReference(Long batchId, String productCode, List<MaterialUse> materials, List<EquipmentUse> equipment,
                                 List<StaffUse> staff, ContainerUse container, ReleaseSignature release,
                                 RejectionNote rejection) {
        public TraceabilityReference {
            materials = List.copyOf(materials);
            equipment = List.copyOf(equipment);
            staff = List.copyOf(staff);
        }
    }

    /**
     * Raw material consumed by the batch; lotId is null for usages recorded before Inventory Management existed.
     */
    record MaterialUse(Long id, Long rawMaterialId, String name, Long lotId, Double quantity, String unit, String usedOn,
                       BigDecimal stockBefore, BigDecimal stockAfter) {
    }

    /** Equipment that took part in the batch. */
    record EquipmentUse(Long equipmentId, String name, String registeredAt) {
    }

    /** Staff member who took part in the batch. */
    record StaffUse(Long staffId, String name, String role, String registeredAt) {
    }

    /** Container monitor of the container where the batch is stored. */
    record ContainerUse(Long containerMonitorId, String name, Long environmentId, Instant assignedAt) {
    }

    /** Release signature of the batch. */
    record ReleaseSignature(Long signedByUserId, String signatureHash, String signedAt) {
    }

    /** Rejection record of the batch. */
    record RejectionNote(String rejectionDate, String reason) {
    }
}
