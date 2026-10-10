package com.iotech.qualitrack.platform.tracking.infrastructure.persistence.jpa.adapters;

import com.iotech.qualitrack.platform.tracking.domain.model.entities.ActuationEvent;
import com.iotech.qualitrack.platform.tracking.domain.model.valueobjects.ActuationAction;
import com.iotech.qualitrack.platform.tracking.domain.repositories.ActuationEventRepository;
import com.iotech.qualitrack.platform.tracking.infrastructure.persistence.jpa.assemblers.ActuationEventPersistenceAssembler;
import com.iotech.qualitrack.platform.tracking.infrastructure.persistence.jpa.repositories.ActuationEventPersistenceRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public class ActuationEventRepositoryImpl implements ActuationEventRepository {
    private final ActuationEventPersistenceRepository persistenceRepository;

    public ActuationEventRepositoryImpl(ActuationEventPersistenceRepository persistenceRepository) {
        this.persistenceRepository = persistenceRepository;
    }

    @Override
    public ActuationEvent save(ActuationEvent event) {
        var saved = persistenceRepository.save(ActuationEventPersistenceAssembler.toPersistenceFromDomain(event));
        return ActuationEventPersistenceAssembler.toDomainFromPersistence(saved);
    }

    @Override
    public List<ActuationEvent> findByDeviceAndPeriod(Long deviceId, Instant from, Instant to) {
        return persistenceRepository.findAllByDeviceIdAndOccurredAtBetweenOrderByOccurredAtAscIdAsc(deviceId, from, to)
                .stream().map(ActuationEventPersistenceAssembler::toDomainFromPersistence).toList();
    }

    @Override
    public List<ActuationEvent> findByEnvironmentAndPeriod(Long laboratoryId, Long environmentId, Instant from, Instant to) {
        return persistenceRepository
                .findAllByLaboratoryIdAndEnvironmentIdAndOccurredAtBetweenOrderByOccurredAtAscIdAsc(laboratoryId, environmentId, from, to)
                .stream().map(ActuationEventPersistenceAssembler::toDomainFromPersistence).toList();
    }

    @Override
    public Optional<ActuationEvent> findByDeviceAndActionAndOccurredAt(Long deviceId, String action, Instant occurredAt) {
        return ActuationAction.parse(action)
                .flatMap(value -> persistenceRepository.findFirstByDeviceIdAndActionAndOccurredAt(deviceId, value, occurredAt))
                .map(ActuationEventPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public Optional<Instant> findLastReceivedAt(Long deviceId) {
        return Optional.ofNullable(persistenceRepository.findLastCreatedAtByDeviceId(deviceId)).map(Date::toInstant);
    }
}
