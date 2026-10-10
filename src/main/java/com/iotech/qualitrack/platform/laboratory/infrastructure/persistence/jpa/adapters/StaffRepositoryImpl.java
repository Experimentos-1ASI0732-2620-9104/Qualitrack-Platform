package com.iotech.qualitrack.platform.laboratory.infrastructure.persistence.jpa.adapters;

import com.iotech.qualitrack.platform.laboratory.domain.model.aggregates.StaffMember;
import com.iotech.qualitrack.platform.laboratory.domain.repositories.StaffRepository;
import com.iotech.qualitrack.platform.laboratory.infrastructure.persistence.jpa.assemblers.StaffPersistenceAssembler;
import com.iotech.qualitrack.platform.laboratory.infrastructure.persistence.jpa.repositories.StaffPersistenceRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JPA adapter implementation for the {@link StaffRepository} port.
 *
 * <p>Translates domain-level requests for staff members into
 * Spring Data JPA database operations using native numeric IDs.</p>
 */
@Repository
public class StaffRepositoryImpl implements StaffRepository {

    private final StaffPersistenceRepository repository;

    public StaffRepositoryImpl(StaffPersistenceRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<StaffMember> findById(Long id) {
        // Native JPA findById using the numeric ID
        return repository.findById(id)
                .map(StaffPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public List<StaffMember> findAll() {
        return repository.findAll().stream()
                .map(StaffPersistenceAssembler::toDomainFromPersistence)
                .toList();
    }

    @Override
    public List<StaffMember> findAllByLaboratoryId(Long laboratoryId) {
        return repository.findAllByLaboratoryId(laboratoryId).stream()
                .map(StaffPersistenceAssembler::toDomainFromPersistence)
                .toList();
    }

    @Override
    public StaffMember save(StaffMember staffMember) {

        var entityToSave = StaffPersistenceAssembler.toPersistenceFromDomain(staffMember);

        var saved = repository.save(entityToSave);

        return StaffPersistenceAssembler.toDomainFromPersistence(saved);
    }

    @Override
    public boolean existsById(Long id) {
        return repository.existsById(id);
    }

    @Override
    public boolean existsByEmail(String email) {
        return repository.existsByEmail(email);
    }

    @Override
    public Optional<StaffMember> findByUserId(Long userId) {
        return repository.findByUserId(userId).map(StaffPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}