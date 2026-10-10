package com.iotech.qualitrack.platform.batch.interfaces.events;

import com.iotech.qualitrack.platform.batch.domain.model.events.BatchReleasedEvent;

/**
 * Integration event published by the Batch bounded context when a production batch is released.
 *
 * <p>This event is intended for other bounded contexts such as Reporting and Audit
 * or Compliance and Alerts.</p>
 *
 * @param batchId the released batch identifier
 * @param laboratoryId the laboratory responsible for the batch
 * @param productId the manufactured product identifier
 * @param batchNumber the traceability code assigned to the batch
 * @param releaseDate the date when the batch was released
 * @param releasedBy the account of the person who released the batch
 */
public record BatchReleasedIntegrationEvent(
        Long batchId,
        Long laboratoryId,
        Long productId,
        String batchNumber,
        String releaseDate,
        Long releasedBy
) {
    /**
     * Creates an integration event from an internal domain event.
     *
     * @param event the internal batch released domain event
     * @return the integration event
     */
    public static BatchReleasedIntegrationEvent from(BatchReleasedEvent event) {
        return new BatchReleasedIntegrationEvent(
                event.batchId(),
                event.laboratoryId(),
                event.productId(),
                event.batchNumber(),
                event.releaseDate(),
                event.releasedBy()
        );
    }
}