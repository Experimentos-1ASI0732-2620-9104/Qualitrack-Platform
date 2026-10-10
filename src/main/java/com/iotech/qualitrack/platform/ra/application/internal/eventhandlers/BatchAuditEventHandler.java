package com.iotech.qualitrack.platform.ra.application.internal.eventhandlers;

import com.iotech.qualitrack.platform.batch.interfaces.events.BatchCreatedIntegrationEvent;
import com.iotech.qualitrack.platform.batch.interfaces.events.BatchParticipationRegisteredIntegrationEvent;
import com.iotech.qualitrack.platform.batch.interfaces.events.BatchStoredInContainerIntegrationEvent;
import com.iotech.qualitrack.platform.batch.interfaces.events.BatchRejectedIntegrationEvent;
import com.iotech.qualitrack.platform.batch.interfaces.events.BatchReleasedIntegrationEvent;
import com.iotech.qualitrack.platform.batch.interfaces.events.BatchStartedIntegrationEvent;
import com.iotech.qualitrack.platform.batch.interfaces.events.ProductCreatedIntegrationEvent;
import com.iotech.qualitrack.platform.batch.interfaces.events.RawMaterialLinkedToBatchIntegrationEvent;
import com.iotech.qualitrack.platform.ra.domain.model.valueobjects.AuditAction;
import com.iotech.qualitrack.platform.ra.interfaces.acl.RaContextFacade;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/**
 * Handles Batch integration events and records audit logs in RA.
 */
@Service
@Slf4j
public class BatchAuditEventHandler {

    private final com.iotech.qualitrack.platform.shared.application.security.CurrentUser currentUser;

    private final RaContextFacade raContextFacade;

    public BatchAuditEventHandler(RaContextFacade raContextFacade,
            com.iotech.qualitrack.platform.shared.application.security.CurrentUser currentUser) {
        this.currentUser = currentUser;
        this.raContextFacade = raContextFacade;
    }

    @EventListener(BatchCreatedIntegrationEvent.class)
    public void on(BatchCreatedIntegrationEvent event) {
        log.info("RA received batch created event: {}", event);

        raContextFacade.recordAuditLog(
                AuditAction.REGISTER,
                "BATCH",
                event.batchId(),
                currentUser.userId(),
                event.toString()
        );
    }

    @EventListener(BatchStartedIntegrationEvent.class)
    public void on(BatchStartedIntegrationEvent event) {
        log.info("RA received batch started event: {}", event);

        raContextFacade.recordAuditLog(
                AuditAction.UPDATE,
                "BATCH",
                event.batchId(),
                currentUser.userId(),
                event.toString()
        );
    }

    @EventListener(BatchReleasedIntegrationEvent.class)
    public void on(BatchReleasedIntegrationEvent event) {
        log.info("RA received batch released event: {}", event);

        raContextFacade.recordAuditLog(
                AuditAction.UPDATE,
                "BATCH",
                event.batchId(),
                currentUser.userId(),
                event.toString()
        );
    }

    @EventListener(BatchRejectedIntegrationEvent.class)
    public void on(BatchRejectedIntegrationEvent event) {
        log.info("RA received batch rejected event: {}", event);

        raContextFacade.recordAuditLog(
                AuditAction.UPDATE,
                "BATCH",
                event.batchId(),
                currentUser.userId(),
                event.toString()
        );
    }

    @EventListener(RawMaterialLinkedToBatchIntegrationEvent.class)
    public void on(RawMaterialLinkedToBatchIntegrationEvent event) {
        log.info("RA received raw material linked to batch event: {}", event);

        raContextFacade.recordAuditLog(
                AuditAction.REGISTER,
                "RAW_MATERIAL_USAGE",
                event.batchId(),
                currentUser.userId(),
                event.toString()
        );
    }

    @EventListener(ProductCreatedIntegrationEvent.class)
    public void on(ProductCreatedIntegrationEvent event) {
        log.info("RA received product created event: {}", event);
        raContextFacade.recordAuditLog(
                AuditAction.REGISTER,
                "PHARMACEUTICAL_PRODUCT",
                event.productId(),
                currentUser.userId(),
                event.toString()
        );
    }

    @EventListener(BatchParticipationRegisteredIntegrationEvent.class)
    public void on(BatchParticipationRegisteredIntegrationEvent event) {
        log.info("RA received batch participation event: {}", event);
        raContextFacade.recordAuditLog(
                AuditAction.REGISTER,
                "BATCH",
                event.batchId(),
                currentUser.userId(),
                event.toString()
        );
    }

    @EventListener(BatchStoredInContainerIntegrationEvent.class)
    public void on(BatchStoredInContainerIntegrationEvent event) {
        log.info("RA received batch storage event: {}", event);
        raContextFacade.recordAuditLog(
                AuditAction.UPDATE,
                "BATCH",
                event.batchId(),
                currentUser.userId(),
                event.toString()
        );
    }
}
