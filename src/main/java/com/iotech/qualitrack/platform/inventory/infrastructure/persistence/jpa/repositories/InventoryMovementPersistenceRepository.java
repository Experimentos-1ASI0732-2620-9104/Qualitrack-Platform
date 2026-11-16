package com.iotech.qualitrack.platform.inventory.infrastructure.persistence.jpa.repositories;

import com.iotech.qualitrack.platform.inventory.infrastructure.persistence.jpa.entities.InventoryMovementPersistenceEntity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InventoryMovementPersistenceRepository extends JpaRepository<InventoryMovementPersistenceEntity, Long> {
    List<InventoryMovementPersistenceEntity> findAllByLaboratoryIdAndMaterialIdOrderByOccurredAtDescIdDesc(Long laboratoryId, Long materialId);
    Optional<InventoryMovementPersistenceEntity> findByLaboratoryIdAndOperationId(Long laboratoryId, String operationId);
}
