package com.iotech.qualitrack.platform.batch.domain.model.events;

import com.iotech.qualitrack.platform.batch.domain.model.aggregates.Batch;

import java.time.Instant;

/**
 * Domain event published when the fabrication of a {@link Batch} starts with its first raw material consumption.
 *
 * @param batchId      The numeric identity of the batch.
 * @param laboratoryId The numeric identity of the laboratory responsible for the batch.
 * @param productId    The numeric identity of the manufactured product.
 * @param batchNumber  The traceability code assigned to the batch.
 * @param startedAt    The moment of the consumption that started the batch.
 */
public record BatchStartedEvent(
        Long batchId,
        Long laboratoryId,
        Long productId,
        String batchNumber,
        Instant startedAt) {

    /**
     * Convenience factory that extracts all needed fields from a started {@link Batch}.
     *
     * @param batch     the started batch
     * @param startedAt the moment of the consumption that started it
     * @return a fully populated {@link BatchStartedEvent}
     */
    public static BatchStartedEvent from(Batch batch, Instant startedAt) {
        return new BatchStartedEvent(
                batch.getId(),
                batch.getLabId(),
                batch.getProductId(),
                batch.getBatchNumber(),
                startedAt
        );
    }
}
