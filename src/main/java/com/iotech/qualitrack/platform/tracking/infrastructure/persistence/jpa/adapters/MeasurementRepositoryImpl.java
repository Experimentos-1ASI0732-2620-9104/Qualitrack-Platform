package com.iotech.qualitrack.platform.tracking.infrastructure.persistence.jpa.adapters;

import com.iotech.qualitrack.platform.tracking.domain.model.entities.Measurement;
import com.iotech.qualitrack.platform.tracking.domain.repositories.MeasurementRepository;
import com.iotech.qualitrack.platform.tracking.infrastructure.persistence.jpa.assemblers.MeasurementPersistenceAssembler;
import com.iotech.qualitrack.platform.tracking.infrastructure.persistence.jpa.repositories.MeasurementPersistenceRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public class MeasurementRepositoryImpl implements MeasurementRepository {
    private final MeasurementPersistenceRepository persistenceRepository;

    public MeasurementRepositoryImpl(MeasurementPersistenceRepository persistenceRepository) {
        this.persistenceRepository = persistenceRepository;
    }

    @Override
    public Measurement save(Measurement measurement) {
        var saved = persistenceRepository.save(MeasurementPersistenceAssembler.toPersistenceFromDomain(measurement));
        return MeasurementPersistenceAssembler.toDomainFromPersistence(saved);
    }

    @Override
    public List<Measurement> findByDeviceAndPeriod(Long deviceId, String metric, Instant from, Instant to) {
        var rows = metric == null
                ? persistenceRepository.findAllByEquipmentIdAndMeasuredAtBetweenOrderByMeasuredAtAscIdAsc(deviceId, from, to)
                : persistenceRepository.findAllByEquipmentIdAndParameterNameAndMeasuredAtBetweenOrderByMeasuredAtAscIdAsc(
                        deviceId, metric, from, to);
        return rows.stream().map(MeasurementPersistenceAssembler::toDomainFromPersistence).toList();
    }

    @Override
    public Optional<Measurement> findByDeviceAndMetricAndMeasuredAt(Long deviceId, String metric, Instant measuredAt) {
        return persistenceRepository.findFirstByEquipmentIdAndParameterNameAndMeasuredAt(deviceId, metric, measuredAt)
                .map(MeasurementPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public Optional<Measurement> findPreviousReading(Long deviceId, String metric, Instant before) {
        return persistenceRepository
                .findFirstByEquipmentIdAndParameterNameAndMeasuredAtBeforeOrderByMeasuredAtDesc(deviceId, metric, before)
                .map(MeasurementPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public List<Measurement> findByEnvironmentAndPeriod(Long laboratoryId, Long environmentId, Instant from, Instant to) {
        return persistenceRepository
                .findAllByLaboratoryIdAndEnvironmentIdAndMeasuredAtBetweenOrderByMeasuredAtAscIdAsc(laboratoryId, environmentId, from, to)
                .stream()
                .map(MeasurementPersistenceAssembler::toDomainFromPersistence)
                .toList();
    }

    @Override
    public Optional<Instant> findLastReceivedAt(Long deviceId) {
        return Optional.ofNullable(persistenceRepository.findLastCreatedAtByEquipmentId(deviceId)).map(Date::toInstant);
    }
}
