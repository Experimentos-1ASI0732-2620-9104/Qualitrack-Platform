package com.iotech.qualitrack.platform.equipment.infrastructure.persistence.jpa.repositories;

import com.iotech.qualitrack.platform.equipment.domain.model.valueobjects.IotDeviceType;
import com.iotech.qualitrack.platform.equipment.infrastructure.persistence.jpa.entities.EquipmentPersistenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link EquipmentPersistenceEntity}.
 *
 * <p>Handles database operations for the equipment table using Long as the primary key.
 * Provides custom queries for fetching equipment by laboratory, IoT sensor ID, and validating unique serial numbers.</p>
 */
@Repository
public interface EquipmentPersistenceRepository extends JpaRepository<EquipmentPersistenceEntity, Long> {

    /**
     * Finds all equipment associated with a specific laboratory.
     * @param labId The numeric ID of the laboratory.
     */
    List<EquipmentPersistenceEntity> findAllByLabId(Long labId);

    /**
     * Finds an equipment by its linked external IoT sensor device ID.
     * @param sensorExternalId The external sensor ID (mapped as String in persistence).
     */
    Optional<EquipmentPersistenceEntity> findBySensorExternalId(String sensorExternalId);

    /**
     * Checks if an equipment with the given serial number already exists.
     * Useful for Fail-Fast validation during equipment registration to prevent duplicates.
     */
    boolean existsBySerialNumber(String serialNumber);

    boolean existsBySensorExternalId(String sensorExternalId);

    boolean existsByEnvironmentIdAndDeviceTypeAndIdNot(Long environmentId, IotDeviceType deviceType, Long id);
}