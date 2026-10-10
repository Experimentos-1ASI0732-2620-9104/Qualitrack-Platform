package com.iotech.qualitrack.platform.equipment.infrastructure.persistence.jpa.adapters;

import com.iotech.qualitrack.platform.equipment.domain.model.entities.EquipmentStatusChange;
import com.iotech.qualitrack.platform.equipment.domain.repositories.EquipmentStatusChangeRepository;
import com.iotech.qualitrack.platform.equipment.infrastructure.persistence.jpa.entities.EquipmentStatusChangePersistenceEntity;
import com.iotech.qualitrack.platform.equipment.infrastructure.persistence.jpa.repositories.EquipmentStatusChangePersistenceRepository;
import org.springframework.stereotype.Repository;

@Repository
public class EquipmentStatusChangeRepositoryImpl implements EquipmentStatusChangeRepository {
    private final EquipmentStatusChangePersistenceRepository repository;

    public EquipmentStatusChangeRepositoryImpl(EquipmentStatusChangePersistenceRepository repository) {
        this.repository = repository;
    }

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
