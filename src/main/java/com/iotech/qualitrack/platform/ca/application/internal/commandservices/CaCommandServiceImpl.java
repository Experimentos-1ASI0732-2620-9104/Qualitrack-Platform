package com.iotech.qualitrack.platform.ca.application.internal.commandservices;

import com.iotech.qualitrack.platform.ca.application.commandservices.CaCommandService;
import com.iotech.qualitrack.platform.ca.application.internal.outboundservices.acl.CaExternalEquipmentService;
import com.iotech.qualitrack.platform.ca.domain.model.aggregates.DeviationAlert;
import com.iotech.qualitrack.platform.ca.domain.model.commands.AcknowledgeAlertCommand;
import com.iotech.qualitrack.platform.ca.domain.model.commands.CreateDeviationAlertCommand;
import com.iotech.qualitrack.platform.ca.domain.model.commands.RecordConditionNormalizedCommand;
import com.iotech.qualitrack.platform.ca.domain.model.commands.ResolveAlertCommand;
import com.iotech.qualitrack.platform.ca.domain.model.commands.UpdateNotificationPreferenceCommand;
import com.iotech.qualitrack.platform.ca.domain.model.entities.ComplianceEvent;
import com.iotech.qualitrack.platform.ca.domain.model.entities.NotificationPreference;
import com.iotech.qualitrack.platform.ca.domain.model.events.DeviationAlertAcknowledgedEvent;
import com.iotech.qualitrack.platform.ca.domain.model.events.DeviationAlertCreatedEvent;
import com.iotech.qualitrack.platform.ca.domain.model.events.DeviationAlertEscalatedEvent;
import com.iotech.qualitrack.platform.ca.domain.model.events.DeviationAlertResolvedEvent;
import com.iotech.qualitrack.platform.ca.domain.model.events.NotificationPreferenceUpdatedEvent;
import com.iotech.qualitrack.platform.ca.domain.model.valueobjects.ComplianceEventType;
import com.iotech.qualitrack.platform.ca.domain.model.valueobjects.DeviationRegistration;
import com.iotech.qualitrack.platform.ca.domain.repositories.ComplianceEventRepository;
import com.iotech.qualitrack.platform.ca.domain.repositories.DeviationAlertRepository;
import com.iotech.qualitrack.platform.ca.domain.repositories.NotificationPreferenceRepository;
import com.iotech.qualitrack.platform.shared.application.result.ApplicationError;
import com.iotech.qualitrack.platform.shared.application.result.Result;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

/**
 * Application service implementation that executes CA commands.
 *
 * <p>Handles the orchestration of creating, acknowledging, and resolving deviation
 * alerts, as well as updating user notification preferences. It also records
 * compliance events for traceability.</p>
 */
@Service
public class CaCommandServiceImpl implements CaCommandService {

    private final DeviationAlertRepository deviationAlertRepository;
    private final ComplianceEventRepository complianceEventRepository;
    private final NotificationPreferenceRepository notificationPreferenceRepository;
    private final CaExternalEquipmentService caExternalEquipmentService;
    private final ApplicationEventPublisher eventPublisher;

    public CaCommandServiceImpl(
            DeviationAlertRepository deviationAlertRepository,
            ComplianceEventRepository complianceEventRepository,
            NotificationPreferenceRepository notificationPreferenceRepository,
            CaExternalEquipmentService caExternalEquipmentService,
            ApplicationEventPublisher eventPublisher
    ) {
        this.deviationAlertRepository = deviationAlertRepository;
        this.complianceEventRepository = complianceEventRepository;
        this.notificationPreferenceRepository = notificationPreferenceRepository;
        this.caExternalEquipmentService = caExternalEquipmentService;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Result<DeviationRegistration, ApplicationError> handle(CreateDeviationAlertCommand command) {
        var source = caExternalEquipmentService.findSource(command.laboratoryId(), command.environmentId(), command.deviceId());
        if (source.isEmpty()) {
            return Result.failure(ApplicationError.validationError("deviceId", command.deviceId() == null
                    ? "The environment has no environmental device"
                    : "The device is not an environmental device or container monitor located in the environment"));
        }
        var device = source.get();

        try {
            var open = deviationAlertRepository.findOpenByEquipmentIdAndParameterName(device.deviceId(), command.parameterName());
            if (open.isPresent()) {
                var alert = open.get();
                var escalated = alert.registerDeviation(command);
                var updatedAlert = deviationAlertRepository.save(alert);

                complianceEventRepository.save(new ComplianceEvent(
                        updatedAlert.getId(),
                        escalated ? ComplianceEventType.DEVIATION_ALERT_ESCALATED : ComplianceEventType.DEVIATION_REPEATED,
                        "%s deviation of '%s' (%s %s) added to the open alert; %d deviations in the incident.".formatted(
                                command.severity(), updatedAlert.getParameterName(), command.recordedValue(),
                                updatedAlert.getUnit(), updatedAlert.getDeviationCount()),
                        Instant.now().toString(),
                        null
                ));

                if (escalated) {
                    eventPublisher.publishEvent(DeviationAlertEscalatedEvent.from(updatedAlert));
                }
                return Result.success(new DeviationRegistration(updatedAlert, false));
            }

            var alert = new DeviationAlert(command, device.deviceId(), device.origin());
            var savedAlert = deviationAlertRepository.save(alert);

            complianceEventRepository.save(new ComplianceEvent(
                    savedAlert.getId(),
                    ComplianceEventType.DEVIATION_ALERT_CREATED,
                    "Deviation alert created for parameter '%s' of '%s'.".formatted(savedAlert.getParameterName(), device.name()),
                    Instant.now().toString(),
                    null
            ));

            eventPublisher.publishEvent(DeviationAlertCreatedEvent.from(savedAlert));

            return Result.success(new DeviationRegistration(savedAlert, true));

        } catch (IllegalArgumentException e) {
            return Result.failure(ApplicationError.validationError("DeviationAlert", e.getMessage()));
        } catch (Exception e) {
            return Result.failure(ApplicationError.unexpected("create-deviation-alert", e.getMessage()));
        }
    }

    @Override
    public Optional<DeviationAlert> handle(RecordConditionNormalizedCommand command) {
        var open = deviationAlertRepository.findOpenByEquipmentIdAndParameterName(command.deviceId(), command.parameterName())
                .filter(alert -> command.laboratoryId().equals(alert.getLaboratoryId()));
        if (open.isEmpty() || !open.get().markNormalized(command.normalizedAt())) {
            return Optional.empty();
        }
        var updatedAlert = deviationAlertRepository.save(open.get());

        complianceEventRepository.save(new ComplianceEvent(
                updatedAlert.getId(),
                ComplianceEventType.DEVIATION_CONDITION_NORMALIZED,
                "Condition of '%s' back to normal; the alert stays open until it is resolved."
                        .formatted(updatedAlert.getParameterName()),
                Instant.now().toString(),
                null
        ));
        return Optional.of(updatedAlert);
    }

    @Override
    public Result<Long, ApplicationError> handle(AcknowledgeAlertCommand command) {
        var alertResult = deviationAlertRepository.findById(command.alertId());

        if (alertResult.isEmpty()) {
            return Result.failure(ApplicationError.notFound(
                    "DeviationAlert",
                    String.valueOf(command.alertId())
            ));
        }

        try {
            var alert = alertResult.get();

            alert.acknowledge(command, Instant.now());

            var updatedAlert = deviationAlertRepository.save(alert);

            complianceEventRepository.save(new ComplianceEvent(
                    updatedAlert.getId(),
                    ComplianceEventType.DEVIATION_ALERT_ACKNOWLEDGED,
                    "Deviation alert acknowledged.",
                    Instant.now().toString(),
                    updatedAlert.getAcknowledgedBy()
            ));

            eventPublisher.publishEvent(DeviationAlertAcknowledgedEvent.from(updatedAlert));

            return Result.success(updatedAlert.getId());

        } catch (IllegalStateException e) {
            // The alert is already acknowledged or resolved (TS75, TS76: 409).
            return Result.failure(ApplicationError.conflict("DeviationAlert", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return Result.failure(ApplicationError.validationError("DeviationAlert", e.getMessage()));
        } catch (Exception e) {
            return Result.failure(ApplicationError.unexpected("acknowledge-alert", e.getMessage()));
        }
    }

    @Override
    public Result<Long, ApplicationError> handle(ResolveAlertCommand command) {
        var alertResult = deviationAlertRepository.findById(command.alertId());

        if (alertResult.isEmpty()) {
            return Result.failure(ApplicationError.notFound(
                    "DeviationAlert",
                    String.valueOf(command.alertId())
            ));
        }

        try {
            var alert = alertResult.get();

            alert.resolve(command, Instant.now());

            var updatedAlert = deviationAlertRepository.save(alert);

            complianceEventRepository.save(new ComplianceEvent(
                    updatedAlert.getId(),
                    ComplianceEventType.DEVIATION_ALERT_RESOLVED,
                    "Deviation alert resolved: %s".formatted(updatedAlert.getResolutionNotes()),
                    Instant.now().toString(),
                    updatedAlert.getResolvedBy()
            ));

            eventPublisher.publishEvent(DeviationAlertResolvedEvent.from(updatedAlert));

            return Result.success(updatedAlert.getId());

        } catch (IllegalStateException e) {
            // The alert is already acknowledged or resolved (TS75, TS76: 409).
            return Result.failure(ApplicationError.conflict("DeviationAlert", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return Result.failure(ApplicationError.validationError("DeviationAlert", e.getMessage()));
        } catch (Exception e) {
            return Result.failure(ApplicationError.unexpected("resolve-alert", e.getMessage()));
        }
    }

    @Override
    public Result<NotificationPreference, ApplicationError> handle(UpdateNotificationPreferenceCommand command) {
        try {
            var preference = notificationPreferenceRepository.findByUserId(command.userId())
                    .orElseGet(() -> new NotificationPreference(command.userId()));

            preference.update(command);

            var updatedPreference = notificationPreferenceRepository.save(preference);

            complianceEventRepository.save(new ComplianceEvent(
                    updatedPreference.getUserId(),
                    ComplianceEventType.NOTIFICATION_PREFERENCE_UPDATED,
                    "Notification preferences updated.",
                    Instant.now().toString(),
                    updatedPreference.getUserId()
            ));

            eventPublisher.publishEvent(NotificationPreferenceUpdatedEvent.from(updatedPreference));

            return Result.success(updatedPreference);

        } catch (IllegalArgumentException e) {
            return Result.failure(ApplicationError.validationError("NotificationPreference", e.getMessage()));
        } catch (Exception e) {
            return Result.failure(ApplicationError.unexpected("update-notification-preference", e.getMessage()));
        }
    }
}