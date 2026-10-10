package com.iotech.qualitrack.platform.tracking.infrastructure.persistence.jpa.repositories;

import com.iotech.qualitrack.platform.tracking.infrastructure.persistence.jpa.entities.MeasurementPersistenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface MeasurementPersistenceRepository extends JpaRepository<MeasurementPersistenceEntity, Long> {

    List<MeasurementPersistenceEntity> findAllByEquipmentIdAndMeasuredAtBetweenOrderByMeasuredAtAscIdAsc(
            Long equipmentId, Instant from, Instant to);

    List<MeasurementPersistenceEntity> findAllByEquipmentIdAndParameterNameAndMeasuredAtBetweenOrderByMeasuredAtAscIdAsc(
            Long equipmentId, String parameterName, Instant from, Instant to);

    Optional<MeasurementPersistenceEntity> findFirstByEquipmentIdAndParameterNameAndMeasuredAt(
            Long equipmentId, String parameterName, Instant measuredAt);

    Optional<MeasurementPersistenceEntity> findFirstByEquipmentIdAndParameterNameAndMeasuredAtBeforeOrderByMeasuredAtDesc(
            Long equipmentId, String parameterName, Instant before);

    List<MeasurementPersistenceEntity> findAllByLaboratoryIdAndEnvironmentIdAndMeasuredAtBetweenOrderByMeasuredAtAscIdAsc(
            Long laboratoryId, Long environmentId, Instant from, Instant to);

    @Query("select max(m.createdAt) from MeasurementPersistenceEntity m where m.equipmentId = :equipmentId")
    Date findLastCreatedAtByEquipmentId(@Param("equipmentId") Long equipmentId);
}
