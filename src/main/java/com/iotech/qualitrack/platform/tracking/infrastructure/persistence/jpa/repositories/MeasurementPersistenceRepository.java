package com.iotech.qualitrack.platform.tracking.infrastructure.persistence.jpa.repositories;

import com.iotech.qualitrack.platform.tracking.infrastructure.persistence.jpa.entities.MeasurementPersistenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data repository of {@code telemetry_measurements}. The period queries include both ends and are ordered by
 * time and then by identifier, so readings of the same moment keep their arrival order.
 */
public interface MeasurementPersistenceRepository extends JpaRepository<MeasurementPersistenceEntity, Long> {

    List<MeasurementPersistenceEntity> findAllByEquipmentIdAndMeasuredAtBetweenOrderByMeasuredAtAscIdAsc(
            Long equipmentId, Instant from, Instant to);

    List<MeasurementPersistenceEntity> findAllByEquipmentIdAndParameterNameAndMeasuredAtBetweenOrderByMeasuredAtAscIdAsc(
            Long equipmentId, String parameterName, Instant from, Instant to);

    Optional<MeasurementPersistenceEntity> findFirstByEquipmentIdAndParameterNameAndMeasuredAt(
            Long equipmentId, String parameterName, Instant measuredAt);

    /**
     * Latest reading of the device and metric strictly before a moment.
     */
    Optional<MeasurementPersistenceEntity> findFirstByEquipmentIdAndParameterNameAndMeasuredAtBeforeOrderByMeasuredAtDesc(
            Long equipmentId, String parameterName, Instant before);

    List<MeasurementPersistenceEntity> findAllByLaboratoryIdAndEnvironmentIdAndMeasuredAtBetweenOrderByMeasuredAtAscIdAsc(
            Long laboratoryId, Long environmentId, Instant from, Instant to);

    /**
     * @return the creation time of the latest reading of the device, or null when it has none
     */
    @Query("select max(m.createdAt) from MeasurementPersistenceEntity m where m.equipmentId = :equipmentId")
    Date findLastCreatedAtByEquipmentId(@Param("equipmentId") Long equipmentId);
}
