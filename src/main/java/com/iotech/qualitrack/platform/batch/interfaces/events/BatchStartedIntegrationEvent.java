package com.iotech.qualitrack.platform.batch.interfaces.events;

import com.iotech.qualitrack.platform.batch.domain.model.events.BatchStartedEvent;

import java.time.Instant;

/**
 * Integration event published by the Batch bounded context when the fabrication of a batch starts with its first
 * raw material consumption.
 *
 * @param batchId the started batch identifier
 * @param laboratoryId the laboratory responsible for the batch
 * @param productId the manufactured product identifier
 * @param batchNumber the traceability code assigned to the batch
 * @param startedAt the moment of the consumption that started the batch
 */
public record BatchStartedIntegrationEvent(
        Long batchId,
        Long laboratoryId,
        Long productId,
        String batchNumber,
        Instant startedAt
) {
    /**
     * Creates an integration event from an internal domain event.
     *
     * @param event the internal batch started domain event
     * @return the integration event
     */
    public static BatchStartedIntegrationEvent from(BatchStartedEvent event) {
        return new BatchStartedIntegrationEvent(
                event.batchId(),
                event.laboratoryId(),
                event.productId(),
                event.batchNumber(),
                event.startedAt()
        );
    }
}
