package com.iotech.qualitrack.platform.equipment.infrastructure.persistence.jpa.repositories;

import com.iotech.qualitrack.platform.equipment.infrastructure.persistence.jpa.entities.MaintenancePersistenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for {@link MaintenancePersistenceEntity}.
 *
 * <p>Handles database operations for the maintenance records table using Long as the primary key.
 * Provides custom queries for fetching the maintenance history of specific equipment.</p>
 */
@Repository
public interface MaintenancePersistenceRepository extends JpaRepository<MaintenancePersistenceEntity, Long> {

    /**
     * Finds all maintenance records associated with a specific equipment.
     * @param equipmentId The numeric ID of the equipment.
     */
    List<MaintenancePersistenceEntity> findAllByEquipmentIdOrderByMaintenanceDateDescIdDesc(Long equipmentId);
}