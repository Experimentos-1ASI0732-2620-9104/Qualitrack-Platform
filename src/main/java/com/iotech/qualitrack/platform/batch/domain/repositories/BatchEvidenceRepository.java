package com.iotech.qualitrack.platform.batch.domain.repositories;

import com.iotech.qualitrack.platform.batch.domain.model.entities.DigitalSignature;
import com.iotech.qualitrack.platform.batch.domain.model.entities.RejectionRecord;

import java.util.Optional;

/**
 * Stores the evidence of the final decision on a batch: the release signature or the rejection record.
 */
public interface BatchEvidenceRepository {
    DigitalSignature saveSignature(DigitalSignature signature);

    RejectionRecord saveRejection(RejectionRecord record);

    Optional<DigitalSignature> findSignatureByBatchId(Long batchId);

    Optional<RejectionRecord> findRejectionByBatchId(Long batchId);
}
