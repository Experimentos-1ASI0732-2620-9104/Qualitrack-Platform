package com.iotech.qualitrack.platform.tracking.infrastructure.persistence.jpa.repositories;

import com.iotech.qualitrack.platform.tracking.domain.model.valueobjects.ActuationAction;
import com.iotech.qualitrack.platform.tracking.infrastructure.persistence.jpa.entities.ActuationEventPersistenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface ActuationEventPersistenceRepository extends JpaRepository<ActuationEventPersistenceEntity, Long> {

    List<ActuationEventPersistenceEntity> findAllByDeviceIdAndOccurredAtBetweenOrderByOccurredAtAscIdAsc(
            Long deviceId, Instant from, Instant to);

    List<ActuationEventPersistenceEntity> findAllByLaboratoryIdAndEnvironmentIdAndOccurredAtBetweenOrderByOccurredAtAscIdAsc(
            Long laboratoryId, Long environmentId, Instant from, Instant to);

    Optional<ActuationEventPersistenceEntity> findFirstByDeviceIdAndActionAndOccurredAt(
            Long deviceId, ActuationAction action, Instant occurredAt);

    @Query("select max(e.createdAt) from ActuationEventPersistenceEntity e where e.deviceId = :deviceId")
    Date findLastCreatedAtByDeviceId(@Param("deviceId") Long deviceId);
}
