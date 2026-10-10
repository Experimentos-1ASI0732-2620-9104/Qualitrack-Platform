package com.iotech.qualitrack.platform.batch.infrastructure.persistence.jpa.adapters;

import com.iotech.qualitrack.platform.batch.domain.model.entities.DigitalSignature;
import com.iotech.qualitrack.platform.batch.domain.model.entities.RejectionRecord;
import com.iotech.qualitrack.platform.batch.domain.repositories.BatchEvidenceRepository;
import com.iotech.qualitrack.platform.batch.infrastructure.persistence.jpa.assemblers.DigitalSignaturePersistenceAssembler;
import com.iotech.qualitrack.platform.batch.infrastructure.persistence.jpa.assemblers.RejectionRecordPersistenceAssembler;
import com.iotech.qualitrack.platform.batch.infrastructure.persistence.jpa.repositories.DigitalSignaturePersistenceRepository;
import com.iotech.qualitrack.platform.batch.infrastructure.persistence.jpa.repositories.RejectionRecordPersistenceRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * JPA adapter that stores release signatures and rejection records of batches.
 */
@Repository
public class BatchEvidenceRepositoryImpl implements BatchEvidenceRepository {

    private final DigitalSignaturePersistenceRepository signatures;
    private final RejectionRecordPersistenceRepository rejections;

    public BatchEvidenceRepositoryImpl(DigitalSignaturePersistenceRepository signatures,
                                       RejectionRecordPersistenceRepository rejections) {
        this.signatures = signatures;
        this.rejections = rejections;
    }

    @Override
    public DigitalSignature saveSignature(DigitalSignature signature) {
        var saved = signatures.save(DigitalSignaturePersistenceAssembler.toPersistenceFromDomain(signature));
        return DigitalSignaturePersistenceAssembler.toDomainFromPersistence(saved);
    }

    @Override
    public RejectionRecord saveRejection(RejectionRecord record) {
        var saved = rejections.save(RejectionRecordPersistenceAssembler.toPersistenceFromDomain(record));
        return RejectionRecordPersistenceAssembler.toDomainFromPersistence(saved);
    }

    @Override
    public Optional<DigitalSignature> findSignatureByBatchId(Long batchId) {
        return signatures.findByBatchId(batchId).map(DigitalSignaturePersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public Optional<RejectionRecord> findRejectionByBatchId(Long batchId) {
        return rejections.findByBatchId(batchId).map(RejectionRecordPersistenceAssembler::toDomainFromPersistence);
    }
}
