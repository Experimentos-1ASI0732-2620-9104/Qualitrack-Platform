package com.iotech.qualitrack.platform.equipment.infrastructure.persistence.jpa.adapters;

import com.iotech.qualitrack.platform.equipment.domain.model.aggregates.Equipment;
import com.iotech.qualitrack.platform.equipment.domain.model.valueobjects.DeviceId;
import com.iotech.qualitrack.platform.equipment.domain.model.valueobjects.IotDeviceType;
import com.iotech.qualitrack.platform.equipment.domain.repositories.EquipmentRepository;
import com.iotech.qualitrack.platform.equipment.infrastructure.persistence.jpa.assemblers.EquipmentPersistenceAssembler;
import com.iotech.qualitrack.platform.equipment.infrastructure.persistence.jpa.repositories.EquipmentPersistenceRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JPA adapter implementation for the {@link EquipmentRepository} port.
 *
 * <p>Translates domain-level requests for equipment into
 * Spring Data JPA database operations.</p>
 */
@Repository
public class EquipmentRepositoryImpl implements EquipmentRepository {

    private final EquipmentPersistenceRepository repository;

    public EquipmentRepositoryImpl(EquipmentPersistenceRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Equipment> findById(Long id) {
        return repository.findById(id)
                .map(EquipmentPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public List<Equipment> findAll() {
        return repository.findAll().stream()
                .map(EquipmentPersistenceAssembler::toDomainFromPersistence)
                .toList();
    }

    @Override
    public List<Equipment> findAllByLabId(Long labId) {
        return repository.findAllByLabId(labId).stream()
                .map(EquipmentPersistenceAssembler::toDomainFromPersistence)
                .toList();
    }

    @Override
    public Optional<Equipment> findBySensorExternalId(DeviceId sensorExternalId) {
        // Extraemos el valor String del Value Object para la consulta JPA
        return repository.findBySensorExternalId(sensorExternalId.value())
                .map(EquipmentPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public Equipment save(Equipment equipment) {
        var entityToSave = EquipmentPersistenceAssembler.toPersistenceFromDomain(equipment);

        var saved = repository.save(entityToSave);

        return EquipmentPersistenceAssembler.toDomainFromPersistence(saved);
    }

    @Override
    public boolean existsById(Long id) {
        return repository.existsById(id);
    }

    @Override
    public boolean existsBySerialNumber(String serialNumber) {
        return repository.existsBySerialNumber(serialNumber);
    }

    @Override
    public boolean existsBySensorExternalId(DeviceId sensorExternalId) {
        return repository.existsBySensorExternalId(sensorExternalId.value());
    }

    @Override
    public boolean existsOtherDeviceInEnvironment(Long environmentId, IotDeviceType deviceType, Long equipmentId) {
        return repository.existsByEnvironmentIdAndDeviceTypeAndIdNot(environmentId, deviceType, equipmentId);
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}