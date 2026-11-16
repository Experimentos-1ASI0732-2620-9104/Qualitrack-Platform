package com.iotech.qualitrack.platform.ra.application.internal.eventhandlers;

import com.iotech.qualitrack.platform.ca.interfaces.events.DeviationAlertAcknowledgedIntegrationEvent;
import com.iotech.qualitrack.platform.ca.interfaces.events.DeviationAlertCreatedIntegrationEvent;
import com.iotech.qualitrack.platform.ca.interfaces.events.DeviationAlertEscalatedIntegrationEvent;
import com.iotech.qualitrack.platform.ca.interfaces.events.DeviationAlertResolvedIntegrationEvent;
import com.iotech.qualitrack.platform.ca.interfaces.events.NotificationPreferenceUpdatedIntegrationEvent;
import com.iotech.qualitrack.platform.ra.domain.model.valueobjects.AuditAction;
import com.iotech.qualitrack.platform.ra.application.commandservices.AuditLogCommandService;
import com.iotech.qualitrack.platform.ra.domain.model.commands.RecordAuditLogEntryCommand;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/**
 * Handles Compliance & Alerts integration events and records audit logs in RA.
 */
@Service
@Slf4j
public class CaAuditEventHandler {

    private final com.iotech.qualitrack.platform.shared.application.security.CurrentUser currentUser;

    private final AuditLogCommandService auditLogCommandService;

    public CaAuditEventHandler(AuditLogCommandService auditLogCommandService,
            com.iotech.qualitrack.platform.shared.application.security.CurrentUser currentUser) {
        this.currentUser = currentUser;
        this.auditLogCommandService = auditLogCommandService;
    }

    /**
     * Records the creation of a deviation alert.
     * Maps to {@link AuditAction#REGISTER} on the {@code DEVIATION_ALERT} aggregate.
     */
    @EventListener(DeviationAlertCreatedIntegrationEvent.class)
    public void on(DeviationAlertCreatedIntegrationEvent event) {
        log.info("RA received deviation alert created event: {}", event);

        auditLogCommandService.handle(new RecordAuditLogEntryCommand(
                AuditAction.REGISTER,
                "DEVIATION_ALERT",
                event.alertId(),
                currentUser.userId(),
                event.toString()
        ));
    }

    /**
     * Records the escalation of a deviation alert.
     * Maps to {@link AuditAction#UPDATE} on {@code DEVIATION_ALERT}.
     */
    @EventListener(DeviationAlertEscalatedIntegrationEvent.class)
    public void on(DeviationAlertEscalatedIntegrationEvent event) {
        log.info("RA received deviation alert escalated event: {}", event);

        auditLogCommandService.handle(new RecordAuditLogEntryCommand(
                AuditAction.UPDATE,
                "DEVIATION_ALERT",
                event.alertId(),
                currentUser.userId(),
                event.toString()
        ));
    }

    /**
     * Records the acknowledgment of a deviation alert.
     * Maps to {@link AuditAction#UPDATE} on {@code DEVIATION_ALERT}.
     */
    @EventListener(DeviationAlertAcknowledgedIntegrationEvent.class)
    public void on(DeviationAlertAcknowledgedIntegrationEvent event) {
        log.info("RA received deviation alert acknowledged event: {}", event);

        auditLogCommandService.handle(new RecordAuditLogEntryCommand(
                AuditAction.UPDATE,
                "DEVIATION_ALERT",
                event.alertId(),
                currentUser.userId(),
                event.toString()
        ));
    }

    /**
     * Records the acknowledgment of a deviation alert.
     * Maps to {@link AuditAction#UPDATE} on {@code DEVIATION_ALERT}.
     */
    @EventListener(DeviationAlertResolvedIntegrationEvent.class)
    public void on(DeviationAlertResolvedIntegrationEvent event) {
        log.info("RA received deviation alert resolved event: {}", event);

        auditLogCommandService.handle(new RecordAuditLogEntryCommand(
                AuditAction.UPDATE,
                "DEVIATION_ALERT",
                event.alertId(),
                currentUser.userId(),
                event.toString()
        ));
    }

    /**
     * Records the update of a user's notification preferences.
     * Audited on its own {@code NOTIFICATION_PREFERENCE} aggregate (identified by
     * {@code preferenceId}) so preference changes remain traceable independently of
     * the alert lifecycle.
     */
    @EventListener(NotificationPreferenceUpdatedIntegrationEvent.class)
    public void on(NotificationPreferenceUpdatedIntegrationEvent event) {
        log.info("RA received notification preference updated event: {}", event);

        auditLogCommandService.handle(new RecordAuditLogEntryCommand(
                AuditAction.UPDATE,
                "NOTIFICATION_PREFERENCE",
                event.preferenceId(),
                currentUser.userId(),
                event.toString()
        ));
    }
}
