package com.iotech.qualitrack.platform.equipment.infrastructure.persistence.jpa.repositories;

import com.iotech.qualitrack.platform.equipment.infrastructure.persistence.jpa.entities.EquipmentStatusChangePersistenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EquipmentStatusChangePersistenceRepository extends JpaRepository<EquipmentStatusChangePersistenceEntity, Long> {
}
