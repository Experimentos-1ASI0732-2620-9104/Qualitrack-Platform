package com.iotech.qualitrack.platform.batch.infrastructure.persistence.jpa.adapters;

import com.iotech.qualitrack.platform.batch.domain.model.entities.RawMaterialUsage;
import com.iotech.qualitrack.platform.batch.domain.repositories.RawMaterialUsageRepository;
import com.iotech.qualitrack.platform.batch.infrastructure.persistence.jpa.assemblers.RawMaterialUsagePersistenceAssembler;
import com.iotech.qualitrack.platform.batch.infrastructure.persistence.jpa.repositories.RawMaterialUsagePersistenceRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JPA adapter implementation for the {@link RawMaterialUsageRepository} port.
 *
 * <p>Translates domain-level requests for raw material usage records into
 * Spring Data JPA database operations.</p>
 */
@Repository
public class RawMaterialUsageRepositoryImpl implements RawMaterialUsageRepository {

    private final RawMaterialUsagePersistenceRepository repository;

    public RawMaterialUsageRepositoryImpl(RawMaterialUsagePersistenceRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<RawMaterialUsage> findById(Long id) {
        return repository.findById(id)
                .map(RawMaterialUsagePersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public List<RawMaterialUsage> findAll() {
        return repository.findAll().stream()
                .map(RawMaterialUsagePersistenceAssembler::toDomainFromPersistence)
                .toList();
    }

    @Override
    public List<RawMaterialUsage> findAllByBatchId(Long batchId) {
        return repository.findAllByBatchId(batchId).stream()
                .map(RawMaterialUsagePersistenceAssembler::toDomainFromPersistence)
                .toList();
    }

    @Override
    public List<RawMaterialUsage> findAllByRawMaterialId(Long rawMaterialId) {
        return repository.findAllByRawMaterialId(rawMaterialId).stream()
                .map(RawMaterialUsagePersistenceAssembler::toDomainFromPersistence)
                .toList();
    }

    @Override
    public Optional<RawMaterialUsage> findByBatchIdAndOperationId(Long batchId, String operationId) {
        return repository.findByBatchIdAndOperationId(batchId, operationId)
                .map(RawMaterialUsagePersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public RawMaterialUsage save(RawMaterialUsage rawMaterialUsage) {
        var entityToSave = RawMaterialUsagePersistenceAssembler.toPersistenceFromDomain(rawMaterialUsage);

        var saved = repository.save(entityToSave);

        return RawMaterialUsagePersistenceAssembler.toDomainFromPersistence(saved);
    }

    @Override
    public boolean existsById(Long id) {
        return repository.existsById(id);
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}