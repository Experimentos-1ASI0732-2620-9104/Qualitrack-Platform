package com.iotech.qualitrack.platform.ra.application.internal.eventhandlers;

import com.iotech.qualitrack.platform.ca.interfaces.events.DeviationAlertAcknowledgedIntegrationEvent;
import com.iotech.qualitrack.platform.ca.interfaces.events.DeviationAlertCreatedIntegrationEvent;
import com.iotech.qualitrack.platform.ca.interfaces.events.DeviationAlertEscalatedIntegrationEvent;
import com.iotech.qualitrack.platform.ca.interfaces.events.DeviationAlertResolvedIntegrationEvent;
import com.iotech.qualitrack.platform.ca.interfaces.events.NotificationPreferenceUpdatedIntegrationEvent;
import com.iotech.qualitrack.platform.ra.domain.model.valueobjects.AuditAction;
import com.iotech.qualitrack.platform.ra.interfaces.acl.RaContextFacade;
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

    private final RaContextFacade raContextFacade;

    public CaAuditEventHandler(RaContextFacade raContextFacade,
            com.iotech.qualitrack.platform.shared.application.security.CurrentUser currentUser) {
        this.currentUser = currentUser;
        this.raContextFacade = raContextFacade;
    }

    @EventListener(DeviationAlertCreatedIntegrationEvent.class)
    public void on(DeviationAlertCreatedIntegrationEvent event) {
        log.info("RA received deviation alert created event: {}", event);

        raContextFacade.recordAuditLog(
                AuditAction.REGISTER,
                "DEVIATION_ALERT",
                event.alertId(),
                currentUser.userId(),
                event.toString()
        );
    }

    @EventListener(DeviationAlertEscalatedIntegrationEvent.class)
    public void on(DeviationAlertEscalatedIntegrationEvent event) {
        log.info("RA received deviation alert escalated event: {}", event);

        raContextFacade.recordAuditLog(
                AuditAction.UPDATE,
                "DEVIATION_ALERT",
                event.alertId(),
                currentUser.userId(),
                event.toString()
        );
    }

    @EventListener(DeviationAlertAcknowledgedIntegrationEvent.class)
    public void on(DeviationAlertAcknowledgedIntegrationEvent event) {
        log.info("RA received deviation alert acknowledged event: {}", event);

        raContextFacade.recordAuditLog(
                AuditAction.UPDATE,
                "DEVIATION_ALERT",
                event.alertId(),
                currentUser.userId(),
                event.toString()
        );
    }

    @EventListener(DeviationAlertResolvedIntegrationEvent.class)
    public void on(DeviationAlertResolvedIntegrationEvent event) {
        log.info("RA received deviation alert resolved event: {}", event);

        raContextFacade.recordAuditLog(
                AuditAction.UPDATE,
                "DEVIATION_ALERT",
                event.alertId(),
                currentUser.userId(),
                event.toString()
        );
    }

    @EventListener(NotificationPreferenceUpdatedIntegrationEvent.class)
    public void on(NotificationPreferenceUpdatedIntegrationEvent event) {
        log.info("RA received notification preference updated event: {}", event);

        raContextFacade.recordAuditLog(
                AuditAction.UPDATE,
                "NOTIFICATION_PREFERENCE",
                event.preferenceId(),
                currentUser.userId(),
                event.toString()
        );
    }
}
