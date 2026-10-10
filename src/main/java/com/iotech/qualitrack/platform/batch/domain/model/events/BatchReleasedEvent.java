package com.iotech.qualitrack.platform.batch.domain.model.events;

import com.iotech.qualitrack.platform.batch.domain.model.aggregates.Batch;

/**
 * Domain event published when a production batch is released.
 *
 * <p>Can be listened to by reporting, audit, or compliance contexts to record
 * the final approval of a manufactured batch.</p>
 *
 * @param batchId      The numeric identity of the released batch.
 * @param laboratoryId The numeric identity of the laboratory responsible for the batch.
 * @param productId    The numeric identity of the manufactured product.
 * @param batchNumber  The traceability code assigned to the batch.
 * @param releaseDate  The date when the batch was released.
 * @param releasedBy   The account of the person who released the batch.
 */
public record BatchReleasedEvent(
        Long batchId,
        Long laboratoryId,
        Long productId,
        String batchNumber,
        String releaseDate,
        Long releasedBy) {

    /**
     * Convenience factory that extracts all needed fields from a released {@link Batch}.
     *
     * @param batch the released batch
     * @param releasedBy the account of the person who released it
     * @return a fully populated {@link BatchReleasedEvent}
     */
    public static BatchReleasedEvent from(Batch batch, Long releasedBy) {
        return new BatchReleasedEvent(
                batch.getId(),
                batch.getLabId(),
                batch.getProductId(),
                batch.getBatchNumber(),
                batch.getEndDate(),
                releasedBy
        );
    }
}