package com.iotech.qualitrack.platform.batch.interfaces.acl;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface BatchContextFacade {
    boolean existsBatchById(Long batchId);

    boolean belongsToLaboratory(Long batchId, Long laboratoryId);

    boolean isBatchReleased(Long batchId);

    boolean isBatchRejected(Long batchId);

    /**
     * Finds a product batch, used by the batch report (US96).
     *
     * @param batchId the product batch
     * @return the batch, or empty when it does not exist
     */
    Optional<BatchReference> findBatch(Long batchId);

    /**
     * Product batches of a laboratory, used by the operational counts of the indicators (US93).
     *
     * @param laboratoryId the laboratory that owns the batches
     * @return the batches; empty when the laboratory has none
     */
    List<BatchReference> findBatches(Long laboratoryId);

    /**
     * Raw materials consumed by a batch as they were recorded, also for older batches without environment (US96).
     *
     * @param batchId the product batch
     * @return the usages; empty when the batch has none
     */
    List<MaterialUse> findMaterialUses(Long batchId);

    /**
     * Resources that took part in a batch (US80, US96): consumed lots, equipment, staff, container and the quality
     * decision.
     *
     * @param batchId the product batch
     * @return the traceability, or empty when the batch does not exist or has no environment (older batches)
     */
    Optional<TraceabilityReference> findTraceability(Long batchId);

    /**
     * Product batch data shared with other bounded contexts.
     *
     * @param laboratoryId the owning laboratory
     * @param status the status name (PENDING, IN_PROGRESS, RELEASED or REJECTED)
     * @param startDate the start date, ISO-8601
     * @param endDate the end date, ISO-8601, or null while the batch is open
     */
    record BatchReference(Long id, Long laboratoryId, String batchNumber, String productName, Double quantity,
                          String unit, String status, String startDate, String endDate, String notes) {
        /**
         * @return true when the batch was released
         */
        public boolean isReleased() {
            return "RELEASED".equals(status);
        }
    }

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
