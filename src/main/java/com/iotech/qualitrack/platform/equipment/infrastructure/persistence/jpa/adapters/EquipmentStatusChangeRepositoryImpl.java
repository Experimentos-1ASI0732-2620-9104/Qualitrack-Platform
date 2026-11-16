package com.iotech.qualitrack.platform.equipment.infrastructure.persistence.jpa.adapters;

import com.iotech.qualitrack.platform.equipment.domain.model.entities.EquipmentStatusChange;
import com.iotech.qualitrack.platform.equipment.domain.repositories.EquipmentStatusChangeRepository;
import com.iotech.qualitrack.platform.equipment.infrastructure.persistence.jpa.entities.EquipmentStatusChangePersistenceEntity;
import com.iotech.qualitrack.platform.equipment.infrastructure.persistence.jpa.repositories.EquipmentStatusChangePersistenceRepository;
import org.springframework.stereotype.Repository;

/**
 * JPA adapter implementation for the {@link EquipmentStatusChangeRepository} port.
 *
 * <p>Stores each status change of an equipment so its history stays traceable.</p>
 */
@Repository
public class EquipmentStatusChangeRepositoryImpl implements EquipmentStatusChangeRepository {
    private final EquipmentStatusChangePersistenceRepository repository;

    /**
     * Creates the adapter with the Spring Data repository of the status changes.
     *
     * @param repository Spring Data repository of the status changes
     */
    public EquipmentStatusChangeRepositoryImpl(EquipmentStatusChangePersistenceRepository repository) {
        this.repository = repository;
    }

    /**
     * Saves a status change and returns it with its generated id.
     *
     * @param statusChange status change to store
     * @return the stored status change
     */
    @Override
    public EquipmentStatusChange save(EquipmentStatusChange statusChange) {
        var entity = new EquipmentStatusChangePersistenceEntity();
        entity.setEquipmentId(statusChange.getEquipmentId());
        entity.setEnvironmentId(statusChange.getEnvironmentId());
        entity.setPreviousStatus(statusChange.getPreviousStatus());
        entity.setNewStatus(statusChange.getNewStatus());
        entity.setReason(statusChange.getReason());
        entity.setChangedByUserId(statusChange.getChangedByUserId());
        entity.setChangedAt(statusChange.getChangedAt());
        var saved = repository.save(entity);
        return new EquipmentStatusChange(saved.getId(), saved.getEquipmentId(), saved.getEnvironmentId(),
                saved.getPreviousStatus(), saved.getNewStatus(), saved.getReason(), saved.getChangedByUserId(),
                saved.getChangedAt());
    }
}
