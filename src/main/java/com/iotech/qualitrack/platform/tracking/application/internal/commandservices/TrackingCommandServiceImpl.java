package com.iotech.qualitrack.platform.tracking.application.internal.commandservices;

import com.iotech.qualitrack.platform.equipment.interfaces.acl.EquipmentContextFacade.DeviceReference;
import com.iotech.qualitrack.platform.shared.application.result.ApplicationError;
import com.iotech.qualitrack.platform.shared.application.result.Result;
import com.iotech.qualitrack.platform.shared.application.security.CurrentUser;
import com.iotech.qualitrack.platform.tracking.application.commandservices.TrackingCommandService;
import com.iotech.qualitrack.platform.tracking.application.internal.outboundservices.acl.TrackingExternalEquipmentService;
import com.iotech.qualitrack.platform.tracking.domain.model.aggregates.EnvironmentalProfile;
import com.iotech.qualitrack.platform.tracking.domain.model.commands.RecordActuationEventCommand;
import com.iotech.qualitrack.platform.tracking.domain.model.commands.RecordMeasurementCommand;
import com.iotech.qualitrack.platform.tracking.domain.model.commands.UpdateActuationRulesCommand;
import com.iotech.qualitrack.platform.tracking.domain.model.commands.UpdateContainerMonitorThresholdsCommand;
import com.iotech.qualitrack.platform.tracking.domain.model.commands.UpdateEnvironmentThresholdsCommand;
import com.iotech.qualitrack.platform.tracking.domain.model.entities.ActuationEvent;
import com.iotech.qualitrack.platform.tracking.domain.model.entities.Measurement;
import com.iotech.qualitrack.platform.tracking.domain.model.events.EnvironmentalConditionNormalizedEvent;
import com.iotech.qualitrack.platform.tracking.domain.model.events.EnvironmentalDeviationDetectedEvent;
import com.iotech.qualitrack.platform.tracking.domain.model.events.EnvironmentalProfileUpdatedEvent;
import com.iotech.qualitrack.platform.tracking.domain.model.events.MeasurementRecordedEvent;
import com.iotech.qualitrack.platform.tracking.domain.model.valueobjects.EnvironmentalState;
import com.iotech.qualitrack.platform.tracking.domain.model.valueobjects.MonitoredMetric.DeviceKind;
import com.iotech.qualitrack.platform.tracking.domain.model.valueobjects.ThresholdEvaluation;
import com.iotech.qualitrack.platform.tracking.domain.repositories.ActuationEventRepository;
import com.iotech.qualitrack.platform.tracking.domain.repositories.EnvironmentalProfileRepository;
import com.iotech.qualitrack.platform.tracking.domain.repositories.MeasurementRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Implementation of the Tracking &amp; Telemetry use cases.
 *
 * <p>The platform evaluates every reading with the current profile of its environment or container monitor, records
 * the resulting state and reports a deviation to Compliance &amp; Alerting only when the condition of the metric gets
 * worse, so a sustained WARNING or CRITICAL condition does not create one alert per reading, and reports when it
 * returns to NORMAL. The devices apply the
 * same profile locally and execute the actions; the platform keeps the record of what they did.</p>
 */
@Service
public class TrackingCommandServiceImpl implements TrackingCommandService {
    /**
     * Clock difference tolerated between the devices and the platform.
     */
    private static final Duration CLOCK_TOLERANCE = Duration.ofMinutes(5);

    private final EnvironmentalProfileRepository profileRepository;
    private final MeasurementRepository measurementRepository;
    private final ActuationEventRepository actuationEventRepository;
    private final TrackingExternalEquipmentService externalEquipmentService;
    private final CurrentUser currentUser;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public TrackingCommandServiceImpl(EnvironmentalProfileRepository profileRepository,
                                      MeasurementRepository measurementRepository,
                                      ActuationEventRepository actuationEventRepository,
                                      TrackingExternalEquipmentService externalEquipmentService,
                                      CurrentUser currentUser, ApplicationEventPublisher eventPublisher,
                                      Clock trackingClock) {
        this.profileRepository = profileRepository;
        this.measurementRepository = measurementRepository;
        this.actuationEventRepository = actuationEventRepository;
        this.externalEquipmentService = externalEquipmentService;
        this.currentUser = currentUser;
        this.eventPublisher = eventPublisher;
        this.clock = trackingClock;
    }

    @Override
    @Transactional
    public Result<EnvironmentalProfile, ApplicationError> handle(UpdateEnvironmentThresholdsCommand command) {
        if (externalEquipmentService.findEnvironmentalDevice(command.laboratoryId(), command.environmentId()).isEmpty()) {
            return Result.failure(ApplicationError.validationError("environmentId",
                    "The environment needs an environmental device before configuring its thresholds"));
        }
        var profile = profileRepository.findByEnvironmentId(command.environmentId())
                .orElseGet(() -> EnvironmentalProfile.forEnvironment(command.laboratoryId(), command.environmentId()));
        return change(profile, value -> value.replaceThresholds(command.thresholds(), currentUser.userId(), clock.instant()));
    }

    @Override
    @Transactional
    public Result<EnvironmentalProfile, ApplicationError> handle(UpdateContainerMonitorThresholdsCommand command) {
        return containerMonitorProfile(command.laboratoryId(), command.environmentId(), command.deviceId())
                .map(profile -> change(profile, value ->
                        value.replaceThresholds(command.thresholds(), currentUser.userId(), clock.instant())))
                .orElseGet(() -> Result.failure(notContainerMonitor()));
    }

    @Override
    @Transactional
    public Result<EnvironmentalProfile, ApplicationError> handle(UpdateActuationRulesCommand command) {
        return containerMonitorProfile(command.laboratoryId(), command.environmentId(), command.deviceId())
                .map(profile -> change(profile, value ->
                        value.replaceActuationRules(command.rules(), currentUser.userId(), clock.instant())))
                .orElseGet(() -> Result.failure(notContainerMonitor()));
    }

    @Override
    @Transactional
    public Result<Recorded<Measurement>, ApplicationError> handle(RecordMeasurementCommand command) {
        var environmental = command.deviceId() == null;
        var device = environmental
                ? externalEquipmentService.findEnvironmentalDevice(command.laboratoryId(), command.environmentId())
                : externalEquipmentService.findContainerMonitor(command.laboratoryId(), command.environmentId(), command.deviceId());
        if (device.isEmpty()) {
            return Result.failure(environmental
                    ? ApplicationError.validationError("environmentId", "The environment has no environmental device")
                    : notContainerMonitor());
        }
        var metric = command.metric();
        if (!metric.isReportedBy(device.get().deviceType())) {
            return Result.failure(ApplicationError.validationError("metric",
                    metric + " is not measured by the " + (environmental ? "environmental device" : "container monitor")));
        }
        if (isFuture(command.measuredAt())) {
            return Result.failure(ApplicationError.validationError("measuredAt", "The measurement time cannot be in the future"));
        }
        var deviceId = device.get().id();
        var existing = measurementRepository.findByDeviceAndMetricAndMeasuredAt(deviceId, metric.name(), command.measuredAt());
        if (existing.isPresent()) return Result.success(new Recorded<>(existing.get(), false));

        var profile = environmental ? profileRepository.findByEnvironmentId(command.environmentId())
                : profileRepository.findByDeviceId(deviceId);
        ThresholdEvaluation evaluation = null;
        if (metric.hasThresholds() && command.value() != null && Double.isFinite(command.value())) {
            evaluation = profile.flatMap(value -> value.evaluate(metric, command.value())).orElse(null);
        }
        var profileVersion = command.profileVersion() != null ? command.profileVersion()
                : evaluation == null ? null : profile.map(EnvironmentalProfile::getVersion).orElse(null);
        Measurement measurement;
        try {
            measurement = Measurement.receive(command.laboratoryId(), command.environmentId(), deviceId, metric,
                    command.value(), command.textValue(), command.measuredAt(), evaluation, profileVersion);
        } catch (IllegalArgumentException exception) {
            return Result.failure(ApplicationError.validationError("measurement", exception.getMessage()));
        }
        var previousState = evaluation == null ? null : measurementRepository
                .findPreviousReading(deviceId, metric.name(), command.measuredAt())
                .map(Measurement::getState).orElse(null);
        var saved = measurementRepository.save(measurement);
        eventPublisher.publishEvent(new MeasurementRecordedEvent(saved.getId(), saved.getLaboratoryId(),
                saved.getEnvironmentId(), deviceId, saved.getParameterName(), saved.getValue(), saved.getTextValue(),
                saved.getUnit(), saved.getMeasuredAt(), saved.getState() == null ? null : saved.getState().name()));
        if (evaluation != null && evaluation.state().worsens(previousState)) {
            eventPublisher.publishEvent(new EnvironmentalDeviationDetectedEvent(saved.getId(), saved.getLaboratoryId(),
                    saved.getEnvironmentId(), deviceId, saved.getParameterName(), saved.getValue(), saved.getUnit(),
                    evaluation.state().name(), evaluation.exceededLimit(), saved.getMeasuredAt()));
        }
        if (evaluation != null && evaluation.state() == EnvironmentalState.NORMAL && previousState != null
                && previousState != EnvironmentalState.NORMAL) {
            eventPublisher.publishEvent(new EnvironmentalConditionNormalizedEvent(saved.getId(), saved.getLaboratoryId(),
                    saved.getEnvironmentId(), deviceId, saved.getParameterName(), saved.getValue(), saved.getUnit(),
                    saved.getMeasuredAt()));
        }
        return Result.success(new Recorded<>(saved, true));
    }

    @Override
    @Transactional
    public Result<Recorded<ActuationEvent>, ApplicationError> handle(RecordActuationEventCommand command) {
        var device = externalEquipmentService.findContainerMonitor(command.laboratoryId(), command.environmentId(),
                command.deviceId());
        if (device.isEmpty()) return Result.failure(notContainerMonitor());
        if (command.triggerMetric() != null && !command.triggerMetric().isReportedBy(DeviceKind.CONTAINER_MONITOR.name())) {
            return Result.failure(ApplicationError.validationError("triggerMetric",
                    command.triggerMetric() + " is not measured by the container monitor"));
        }
        if (isFuture(command.occurredAt())) {
            return Result.failure(ApplicationError.validationError("occurredAt", "The time of the action cannot be in the future"));
        }
        var existing = actuationEventRepository.findByDeviceAndActionAndOccurredAt(command.deviceId(),
                command.action().name(), command.occurredAt());
        if (existing.isPresent()) return Result.success(new Recorded<>(existing.get(), false));
        try {
            var event = ActuationEvent.record(command.laboratoryId(), command.environmentId(), command.deviceId(),
                    command.action(), command.triggerMetric(), command.triggerState(), command.result(),
                    command.occurredAt(), command.profileVersion());
            return Result.success(new Recorded<>(actuationEventRepository.save(event), true));
        } catch (IllegalArgumentException exception) {
            return Result.failure(ApplicationError.validationError("actuationEvent", exception.getMessage()));
        }
    }

    private Optional<EnvironmentalProfile> containerMonitorProfile(Long laboratoryId, Long environmentId, Long deviceId) {
        return externalEquipmentService.findContainerMonitor(laboratoryId, environmentId, deviceId)
                .map(DeviceReference::id)
                .map(id -> profileRepository.findByDeviceId(id)
                        .orElseGet(() -> EnvironmentalProfile.forContainerMonitor(laboratoryId, id)));
    }

    private Result<EnvironmentalProfile, ApplicationError> change(EnvironmentalProfile profile,
                                                                 Consumer<EnvironmentalProfile> change) {
        try {
            change.accept(profile);
        } catch (IllegalArgumentException exception) {
            return Result.failure(ApplicationError.validationError("environmentalProfile", exception.getMessage()));
        }
        var saved = profileRepository.save(profile);
        eventPublisher.publishEvent(new EnvironmentalProfileUpdatedEvent(saved.getId(), saved.getLaboratoryId(),
                saved.getScope().name(), saved.getEnvironmentId(), saved.getDeviceId(), saved.getVersion(),
                saved.getUpdatedBy()));
        return Result.success(saved);
    }

    private boolean isFuture(Instant moment) {
        return moment.isAfter(clock.instant().plus(CLOCK_TOLERANCE));
    }

    private static ApplicationError notContainerMonitor() {
        return ApplicationError.validationError("deviceId", "The device is not a container monitor located in the environment");
    }
}
