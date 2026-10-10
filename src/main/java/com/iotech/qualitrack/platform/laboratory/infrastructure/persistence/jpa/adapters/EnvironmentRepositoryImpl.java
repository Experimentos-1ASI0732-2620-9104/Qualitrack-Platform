package com.iotech.qualitrack.platform.laboratory.infrastructure.persistence.jpa.adapters;

import com.iotech.qualitrack.platform.laboratory.domain.model.aggregates.Environment;
import com.iotech.qualitrack.platform.laboratory.domain.repositories.EnvironmentRepository;
import com.iotech.qualitrack.platform.laboratory.infrastructure.persistence.jpa.assemblers.EnvironmentPersistenceAssembler;
import com.iotech.qualitrack.platform.laboratory.infrastructure.persistence.jpa.repositories.EnvironmentPersistenceRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JPA adapter implementation for the {@link EnvironmentRepository} port.
 *
 * <p>Updates reuse the managed persistence entity so auditing columns such as
 * {@code createdAt} are preserved.</p>
 */
@Repository
public class EnvironmentRepositoryImpl implements EnvironmentRepository {

    private final EnvironmentPersistenceRepository repository;

    public EnvironmentRepositoryImpl(EnvironmentPersistenceRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Environment> findById(Long id) {
        return repository.findById(id).map(EnvironmentPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public Optional<Environment> findByIdAndLaboratoryId(Long id, Long laboratoryId) {
        return repository.findByIdAndLaboratoryId(id, laboratoryId)
                .map(EnvironmentPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public List<Environment> findAllByLaboratoryId(Long laboratoryId) {
        return repository.findAllByLaboratoryId(laboratoryId).stream()
                .map(EnvironmentPersistenceAssembler::toDomainFromPersistence)
                .toList();
    }

    @Override
    public boolean existsByLaboratoryIdAndCode(Long laboratoryId, String code) {
        return repository.existsByLaboratoryIdAndCode(laboratoryId, code);
    }

    @Override
    public boolean existsByLaboratoryIdAndCodeAndIdNot(Long laboratoryId, String code, Long id) {
        return repository.existsByLaboratoryIdAndCodeAndIdNot(laboratoryId, code, id);
    }

    @Override
    public Environment save(Environment environment) {
        var existing = environment.getId() == null ? null : repository.findById(environment.getId()).orElse(null);
        var saved = repository.save(EnvironmentPersistenceAssembler.toPersistenceFromDomain(environment, existing));
        return EnvironmentPersistenceAssembler.toDomainFromPersistence(saved);
    }
}
