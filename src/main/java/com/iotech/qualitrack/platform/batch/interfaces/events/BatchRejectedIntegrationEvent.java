package com.iotech.qualitrack.platform.batch.interfaces.events;

import com.iotech.qualitrack.platform.batch.domain.model.events.BatchRejectedEvent;

/**
 * Integration event published by the Batch bounded context when a production batch is rejected.
 *
 * <p>This event allows compliance, reporting, and audit processes to react to a
 * rejected production batch without depending on Batch internal domain events.</p>
 *
 * @param batchId the rejected batch identifier
 * @param laboratoryId the laboratory responsible for the batch
 * @param productId the manufactured product identifier
 * @param batchNumber the traceability code assigned to the batch
 * @param rejectionDate the date when the batch was rejected
 * @param reason the rejection reason
 * @param rejectedBy the account of the person who rejected the batch
 */
public record BatchRejectedIntegrationEvent(
        Long batchId,
        Long laboratoryId,
        Long productId,
        String batchNumber,
        String rejectionDate,
        String reason,
        Long rejectedBy
) {
    /**
     * Creates an integration event from an internal domain event.
     *
     * @param event the internal batch rejected domain event
     * @return the integration event
     */
    public static BatchRejectedIntegrationEvent from(BatchRejectedEvent event) {
        return new BatchRejectedIntegrationEvent(
                event.batchId(),
                event.laboratoryId(),
                event.productId(),
                event.batchNumber(),
                event.rejectionDate(),
                event.reason(),
                event.rejectedBy()
        );
    }
}