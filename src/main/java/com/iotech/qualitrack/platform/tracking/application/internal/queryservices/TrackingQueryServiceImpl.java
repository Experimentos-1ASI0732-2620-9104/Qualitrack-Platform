package com.iotech.qualitrack.platform.tracking.application.internal.queryservices;

import com.iotech.qualitrack.platform.tracking.application.internal.outboundservices.acl.TrackingExternalEquipmentService;
import com.iotech.qualitrack.platform.tracking.application.queryservices.TrackingQueryService;
import com.iotech.qualitrack.platform.tracking.domain.model.aggregates.EnvironmentalProfile;
import com.iotech.qualitrack.platform.tracking.domain.model.entities.ActuationEvent;
import com.iotech.qualitrack.platform.tracking.domain.model.entities.Measurement;
import com.iotech.qualitrack.platform.tracking.domain.model.queries.GetActuationEventsQuery;
import com.iotech.qualitrack.platform.tracking.domain.model.queries.GetContainerMonitorProfileQuery;
import com.iotech.qualitrack.platform.tracking.domain.model.queries.GetDeviceConnectionQuery;
import com.iotech.qualitrack.platform.tracking.domain.model.queries.GetDeviceProfileQuery;
import com.iotech.qualitrack.platform.tracking.domain.model.queries.GetEnvironmentProfileQuery;
import com.iotech.qualitrack.platform.tracking.domain.model.queries.GetMeasurementsQuery;
import com.iotech.qualitrack.platform.tracking.domain.model.valueobjects.DeviceConnection;
import com.iotech.qualitrack.platform.tracking.domain.model.valueobjects.ExpectedCommunicationPeriod;
import com.iotech.qualitrack.platform.tracking.domain.model.valueobjects.MonitoredMetric.DeviceKind;
import com.iotech.qualitrack.platform.tracking.domain.repositories.ActuationEventRepository;
import com.iotech.qualitrack.platform.tracking.domain.repositories.EnvironmentalProfileRepository;
import com.iotech.qualitrack.platform.tracking.domain.repositories.MeasurementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Service
public class TrackingQueryServiceImpl implements TrackingQueryService {
    private final EnvironmentalProfileRepository profileRepository;
    private final MeasurementRepository measurementRepository;
    private final ActuationEventRepository actuationEventRepository;
    private final TrackingExternalEquipmentService externalEquipmentService;
    private final ExpectedCommunicationPeriod expectedCommunicationPeriod;
    private final Clock clock;

    public TrackingQueryServiceImpl(EnvironmentalProfileRepository profileRepository,
                                    MeasurementRepository measurementRepository,
                                    ActuationEventRepository actuationEventRepository,
                                    TrackingExternalEquipmentService externalEquipmentService,
                                    ExpectedCommunicationPeriod expectedCommunicationPeriod, Clock trackingClock) {
        this.profileRepository = profileRepository;
        this.measurementRepository = measurementRepository;
        this.actuationEventRepository = actuationEventRepository;
        this.externalEquipmentService = externalEquipmentService;
        this.expectedCommunicationPeriod = expectedCommunicationPeriod;
        this.clock = trackingClock;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EnvironmentalProfile> handle(GetEnvironmentProfileQuery query) {
        return profileRepository.findByEnvironmentId(query.environmentId())
                .filter(profile -> profile.getLaboratoryId().equals(query.laboratoryId()));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EnvironmentalProfile> handle(GetContainerMonitorProfileQuery query) {
        return externalEquipmentService.findContainerMonitor(query.laboratoryId(), query.environmentId(), query.deviceId())
                .flatMap(device -> profileRepository.findByDeviceId(device.id()));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EnvironmentalProfile> handle(GetDeviceProfileQuery query) {
        return externalEquipmentService.findDevice(query.laboratoryId(), query.environmentId(), query.deviceId())
                .flatMap(device -> DeviceKind.CONTAINER_MONITOR.name().equals(device.deviceType())
                        ? profileRepository.findByDeviceId(device.id())
                        : handle(new GetEnvironmentProfileQuery(query.laboratoryId(), query.environmentId())));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<List<Measurement>> handle(GetMeasurementsQuery query) {
        var device = query.deviceId() == null
                ? externalEquipmentService.findEnvironmentalDevice(query.laboratoryId(), query.environmentId())
                : externalEquipmentService.findContainerMonitor(query.laboratoryId(), query.environmentId(), query.deviceId());
        return device.map(value -> measurementRepository.findByDeviceAndPeriod(value.id(),
                query.metric() == null ? null : query.metric().name(), query.from(), query.to()));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<List<ActuationEvent>> handle(GetActuationEventsQuery query) {
        return externalEquipmentService.findContainerMonitor(query.laboratoryId(), query.environmentId(), query.deviceId())
                .map(device -> actuationEventRepository.findByDeviceAndPeriod(device.id(), query.from(), query.to()));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DeviceConnection> handle(GetDeviceConnectionQuery query) {
        return externalEquipmentService.findDevice(query.laboratoryId(), query.environmentId(), query.deviceId())
                .map(device -> {
                    // A reading or an action received from the device counts as communication.
                    var lastCommunication = Stream.of(
                                    measurementRepository.findLastReceivedAt(device.id()),
                                    actuationEventRepository.findLastReceivedAt(device.id()))
                            .flatMap(Optional::stream)
                            .max(Comparator.<Instant>naturalOrder())
                            .orElse(null);
                    return DeviceConnection.evaluate(device.id(), lastCommunication, clock.instant(),
                            expectedCommunicationPeriod);
                });
    }
}
