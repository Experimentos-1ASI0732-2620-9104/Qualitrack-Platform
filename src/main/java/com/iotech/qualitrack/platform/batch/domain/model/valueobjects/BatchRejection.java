package com.iotech.qualitrack.platform.batch.domain.model.valueobjects;

import com.iotech.qualitrack.platform.batch.domain.model.aggregates.Batch;
import com.iotech.qualitrack.platform.batch.domain.model.entities.RejectionRecord;

/**
 * Outcome of a batch rejection: the rejected batch and the record that keeps the reason (US82).
 *
 * @param batch the rejected batch
 * @param record the stored rejection record
 */
public record BatchRejection(Batch batch, RejectionRecord record) {
}
