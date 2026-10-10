package com.iotech.qualitrack.platform.ca.application.internal.eventhandlers;

import com.iotech.qualitrack.platform.ca.application.commandservices.NotificationCommandService;
import com.iotech.qualitrack.platform.ca.application.internal.notifications.CriticalAlertEmailScheduler;
import com.iotech.qualitrack.platform.ca.application.internal.outboundservices.acl.CaExternalEquipmentService;
import com.iotech.qualitrack.platform.ca.application.internal.outboundservices.acl.CaExternalLaboratoryService;
import com.iotech.qualitrack.platform.ca.application.internal.outboundservices.acl.CaExternalProfileService;
import com.iotech.qualitrack.platform.ca.domain.model.aggregates.DeviationAlert;
import com.iotech.qualitrack.platform.ca.domain.model.commands.PublishNotificationCommand;
import com.iotech.qualitrack.platform.ca.domain.model.events.DeviationAlertAcknowledgedEvent;
import com.iotech.qualitrack.platform.ca.domain.model.events.DeviationAlertCreatedEvent;
import com.iotech.qualitrack.platform.ca.domain.model.events.DeviationAlertEscalatedEvent;
import com.iotech.qualitrack.platform.ca.domain.model.events.DeviationAlertResolvedEvent;
import com.iotech.qualitrack.platform.ca.domain.model.valueobjects.AlertSeverity;
import com.iotech.qualitrack.platform.ca.domain.model.valueobjects.NotificationContent;
import com.iotech.qualitrack.platform.ca.domain.model.valueobjects.NotificationType;
import com.iotech.qualitrack.platform.ca.domain.repositories.DeviationAlertRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/**
 * Notifies the laboratory of each step of an alert (US83): opened, escalated, acknowledged and resolved. Critical
 * alerts are also e-mailed (US84). A failure to notify never undoes the change of the alert.
 */
@Slf4j
@Service
public class AlertNotificationEventHandler {

    private final DeviationAlertRepository deviationAlertRepository;
    private final NotificationCommandService notificationCommandService;
    private final CriticalAlertEmailScheduler criticalAlertEmailScheduler;
    private final CaExternalLaboratoryService externalLaboratoryService;
    private final CaExternalEquipmentService externalEquipmentService;
    private final CaExternalProfileService externalProfileService;

    public AlertNotificationEventHandler(DeviationAlertRepository deviationAlertRepository,
                                         NotificationCommandService notificationCommandService,
                                         CriticalAlertEmailScheduler criticalAlertEmailScheduler,
                                         CaExternalLaboratoryService externalLaboratoryService,
                                         CaExternalEquipmentService externalEquipmentService,
                                         CaExternalProfileService externalProfileService) {
        this.deviationAlertRepository = deviationAlertRepository;
        this.notificationCommandService = notificationCommandService;
        this.criticalAlertEmailScheduler = criticalAlertEmailScheduler;
        this.externalLaboratoryService = externalLaboratoryService;
        this.externalEquipmentService = externalEquipmentService;
        this.externalProfileService = externalProfileService;
    }

    @EventListener(DeviationAlertCreatedEvent.class)
    public void on(DeviationAlertCreatedEvent event) {
        notify(event.alertId(), NotificationType.ALERT_OPENED, null);
        if (event.severity() == AlertSeverity.CRITICAL) criticalAlertEmailScheduler.schedule(event.alertId());
    }

    @EventListener(DeviationAlertEscalatedEvent.class)
    public void on(DeviationAlertEscalatedEvent event) {
        notify(event.alertId(), NotificationType.ALERT_ESCALATED, null);
        if (event.severity() == AlertSeverity.CRITICAL) criticalAlertEmailScheduler.schedule(event.alertId());
    }

    @EventListener(DeviationAlertAcknowledgedEvent.class)
    public void on(DeviationAlertAcknowledgedEvent event) {
        notify(event.alertId(), NotificationType.ALERT_ACKNOWLEDGED, event.acknowledgedBy());
    }

    @EventListener(DeviationAlertResolvedEvent.class)
    public void on(DeviationAlertResolvedEvent event) {
        notify(event.alertId(), NotificationType.ALERT_RESOLVED, event.resolvedBy());
    }

    private void notify(Long alertId, NotificationType type, Long actorUserId) {
        try {
            deviationAlertRepository.findById(alertId).ifPresent(alert -> notificationCommandService.handle(
                    new PublishNotificationCommand(alert.getLaboratoryId(), contentOf(alert, type, actorUserId), actorUserId)));
        } catch (RuntimeException exception) {
            log.warn("Notification {} of alert {} not created: {}", type, alertId, exception.getMessage());
        }
    }

    private NotificationContent contentOf(DeviationAlert alert, NotificationType type, Long actorUserId) {
        var deviceName = externalEquipmentService.findSource(alert.getLaboratoryId(), alert.getEnvironmentId(),
                alert.getEquipmentId()).map(CaExternalEquipmentService.AlertSource::name).orElse(null);
        return new NotificationContent(type, alert.getSeverity(), alert.getId(),
                externalLaboratoryService.environmentName(alert.getLaboratoryId(), alert.getEnvironmentId()),
                deviceName, alert.getParameterName(), alert.getRecordedValue(), alert.getUnit(),
                actorUserId == null ? null : externalProfileService.displayNameOf(actorUserId),
                type == NotificationType.ALERT_RESOLVED ? alert.getResolutionNotes() : null);
    }
}
