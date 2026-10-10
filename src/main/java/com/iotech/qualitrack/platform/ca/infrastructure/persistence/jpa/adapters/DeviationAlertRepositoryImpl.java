package com.iotech.qualitrack.platform.ca.infrastructure.persistence.jpa.adapters;

import com.iotech.qualitrack.platform.ca.domain.model.aggregates.DeviationAlert;
import com.iotech.qualitrack.platform.ca.domain.model.valueobjects.AlertSeverity;
import com.iotech.qualitrack.platform.ca.domain.model.valueobjects.AlertStatus;
import com.iotech.qualitrack.platform.ca.domain.repositories.DeviationAlertRepository;
import com.iotech.qualitrack.platform.ca.infrastructure.persistence.jpa.assemblers.DeviationAlertPersistenceAssembler;
import com.iotech.qualitrack.platform.ca.infrastructure.persistence.jpa.repositories.DeviationAlertPersistenceRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JPA adapter implementation for the {@link DeviationAlertRepository} port.
 *
 * <p>Translates domain-level requests for deviation alerts into
 * Spring Data JPA database operations.</p>
 */
@Repository
public class DeviationAlertRepositoryImpl implements DeviationAlertRepository {

    private final DeviationAlertPersistenceRepository repository;

    public DeviationAlertRepositoryImpl(DeviationAlertPersistenceRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<DeviationAlert> findById(Long id) {
        return repository.findById(id)
                .map(DeviationAlertPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public List<DeviationAlert> findAll() {
        return repository.findAll().stream()
                .map(DeviationAlertPersistenceAssembler::toDomainFromPersistence)
                .toList();
    }

    @Override
    public List<DeviationAlert> findAllByEquipmentId(Long equipmentId) {
        return repository.findAllByEquipmentId(equipmentId).stream()
                .map(DeviationAlertPersistenceAssembler::toDomainFromPersistence)
                .toList();
    }

    @Override
    public List<DeviationAlert> findAllByBatchId(Long batchId) {
        return repository.findAllByBatchId(batchId).stream()
                .map(DeviationAlertPersistenceAssembler::toDomainFromPersistence)
                .toList();
    }

    @Override
    public List<DeviationAlert> findAllByStatus(AlertStatus status) {
        return repository.findAllByStatus(status).stream()
                .map(DeviationAlertPersistenceAssembler::toDomainFromPersistence)
                .toList();
    }

    @Override
    public List<DeviationAlert> findAllBySeverity(AlertSeverity severity) {
        return repository.findAllBySeverity(severity).stream()
                .map(DeviationAlertPersistenceAssembler::toDomainFromPersistence)
                .toList();
    }

    @Override
    public List<DeviationAlert> findAllByLaboratoryIdAndEnvironmentId(Long laboratoryId, Long environmentId) {
        return repository.findAllByLaboratoryIdAndEnvironmentIdOrderByIdDesc(laboratoryId, environmentId).stream()
                .map(DeviationAlertPersistenceAssembler::toDomainFromPersistence)
                .toList();
    }

    @Override
    public Optional<DeviationAlert> findOpenByEquipmentIdAndParameterName(Long equipmentId, String parameterName) {
        return repository.findFirstByEquipmentIdAndParameterNameAndStatusInOrderByIdDesc(equipmentId, parameterName,
                        List.of(AlertStatus.UNRESOLVED, AlertStatus.ACKNOWLEDGED))
                .map(DeviationAlertPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public DeviationAlert save(DeviationAlert deviationAlert) {
        var entityToSave = DeviationAlertPersistenceAssembler.toPersistenceFromDomain(deviationAlert);

        var saved = repository.save(entityToSave);

        return DeviationAlertPersistenceAssembler.toDomainFromPersistence(saved);
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