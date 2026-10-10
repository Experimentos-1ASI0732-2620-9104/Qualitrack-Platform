package com.iotech.qualitrack.platform.batch.domain.model.valueobjects;

import com.iotech.qualitrack.platform.batch.domain.model.aggregates.Batch;
import com.iotech.qualitrack.platform.batch.domain.model.entities.DigitalSignature;

/**
 * Outcome of a batch release: the released batch and the signature of the user who confirmed it.
 *
 * @param batch the released batch
 * @param signature the stored release signature
 */
public record BatchRelease(Batch batch, DigitalSignature signature) {
}
