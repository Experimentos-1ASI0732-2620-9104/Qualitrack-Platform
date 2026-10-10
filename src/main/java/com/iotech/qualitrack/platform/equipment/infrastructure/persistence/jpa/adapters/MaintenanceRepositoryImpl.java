package com.iotech.qualitrack.platform.equipment.infrastructure.persistence.jpa.adapters;

import com.iotech.qualitrack.platform.equipment.domain.model.aggregates.MaintenanceRecord;
import com.iotech.qualitrack.platform.equipment.domain.repositories.MaintenanceRepository;
import com.iotech.qualitrack.platform.equipment.infrastructure.persistence.jpa.assemblers.MaintenancePersistenceAssembler;
import com.iotech.qualitrack.platform.equipment.infrastructure.persistence.jpa.repositories.MaintenancePersistenceRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JPA adapter implementation for the {@link MaintenanceRepository} port.
 *
 * <p>Translates domain-level requests for maintenance records into
 * Spring Data JPA database operations.</p>
 */
@Repository
public class MaintenanceRepositoryImpl implements MaintenanceRepository {

    private final MaintenancePersistenceRepository repository;

    public MaintenanceRepositoryImpl(MaintenancePersistenceRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<MaintenanceRecord> findById(Long id) {
        return repository.findById(id)
                .map(MaintenancePersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public List<MaintenanceRecord> findAll() {
        return repository.findAll().stream()
                .map(MaintenancePersistenceAssembler::toDomainFromPersistence)
                .toList();
    }

    @Override
    public List<MaintenanceRecord> findAllByEquipmentId(Long equipmentId) {
        return repository.findAllByEquipmentIdOrderByMaintenanceDateDescIdDesc(equipmentId).stream()
                .map(MaintenancePersistenceAssembler::toDomainFromPersistence)
                .toList();
    }

    @Override
    public MaintenanceRecord save(MaintenanceRecord maintenanceRecord) {
        var entityToSave = MaintenancePersistenceAssembler.toPersistenceFromDomain(maintenanceRecord);

        var saved = repository.save(entityToSave);

        return MaintenancePersistenceAssembler.toDomainFromPersistence(saved);
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