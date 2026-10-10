package com.iotech.qualitrack.platform.tracking.application.acl;

import com.iotech.qualitrack.platform.tracking.domain.model.entities.ActuationEvent;
import com.iotech.qualitrack.platform.tracking.domain.repositories.ActuationEventRepository;
import com.iotech.qualitrack.platform.tracking.domain.repositories.MeasurementRepository;
import com.iotech.qualitrack.platform.tracking.interfaces.acl.TrackingContextFacade;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Implementation of {@link TrackingContextFacade} over the Tracking repositories.
 */
@Service
public class TrackingContextFacadeImpl implements TrackingContextFacade {
    private final ActuationEventRepository actuationEventRepository;
    private final MeasurementRepository measurementRepository;

    public TrackingContextFacadeImpl(ActuationEventRepository actuationEventRepository,
                                     MeasurementRepository measurementRepository) {
        this.actuationEventRepository = actuationEventRepository;
        this.measurementRepository = measurementRepository;
    }

    @Override
    public List<ActuationReference> findActuations(Long laboratoryId, Long deviceId, String metric, Instant from, Instant to) {
        if (laboratoryId == null || deviceId == null || metric == null || invalid(from, to)) {
            return List.of();
        }
        return actuationEventRepository.findByDeviceAndPeriod(deviceId, from, to).stream()
                .filter(event -> laboratoryId.equals(event.getLaboratoryId()))
                .filter(event -> event.getTriggerMetric() != null && metric.equals(event.getTriggerMetric().name()))
                .map(TrackingContextFacadeImpl::reference)
                .toList();
    }

    @Override
    public List<ActuationReference> findEnvironmentActuations(Long laboratoryId, Long environmentId, Instant from, Instant to) {
        if (laboratoryId == null || environmentId == null || invalid(from, to)) return List.of();
        return actuationEventRepository.findByEnvironmentAndPeriod(laboratoryId, environmentId, from, to).stream()
                .map(TrackingContextFacadeImpl::reference)
                .toList();
    }

    @Override
    public List<MeasurementReference> findMeasurements(Long laboratoryId, Long environmentId, Instant from, Instant to) {
        if (laboratoryId == null || environmentId == null || invalid(from, to)) return List.of();
        return measurementRepository.findByEnvironmentAndPeriod(laboratoryId, environmentId, from, to).stream()
                .map(reading -> new MeasurementReference(reading.getId(), reading.getEquipmentId(),
                        reading.getParameterName(), reading.getValue(), reading.getUnit(),
                        reading.getState() == null ? null : reading.getState().name(), reading.getMeasuredAt()))
                .toList();
    }

    private static boolean invalid(Instant from, Instant to) {
        return from == null || to == null || to.isBefore(from);
    }

    private static ActuationReference reference(ActuationEvent event) {
        return new ActuationReference(event.getId(), event.getDeviceId(), event.getAction().name(),
                event.getTriggerMetric() == null ? null : event.getTriggerMetric().name(),
                event.getTriggerState() == null ? null : event.getTriggerState().name(),
                event.getResult().name(), event.getOccurredAt());
    }
}
