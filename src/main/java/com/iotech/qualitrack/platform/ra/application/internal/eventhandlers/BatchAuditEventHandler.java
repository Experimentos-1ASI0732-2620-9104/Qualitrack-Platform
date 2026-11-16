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
import com.iotech.qualitrack.platform.ra.application.commandservices.AuditLogCommandService;
import com.iotech.qualitrack.platform.ra.domain.model.commands.RecordAuditLogEntryCommand;
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

    private final AuditLogCommandService auditLogCommandService;

    public BatchAuditEventHandler(AuditLogCommandService auditLogCommandService,
            com.iotech.qualitrack.platform.shared.application.security.CurrentUser currentUser) {
        this.currentUser = currentUser;
        this.auditLogCommandService = auditLogCommandService;
    }

    /**
     * Records the creation of a batch. Maps to {@link AuditAction#REGISTER} on the
     * {@code BATCH} aggregate.
     */
    @EventListener(BatchCreatedIntegrationEvent.class)
    public void on(BatchCreatedIntegrationEvent event) {
        log.info("RA received batch created event: {}", event);

        auditLogCommandService.handle(new RecordAuditLogEntryCommand(
                AuditAction.REGISTER,
                "BATCH",
                event.batchId(),
                currentUser.userId(),
                event.toString()
        ));
    }

    /**
     * Records the start of a batch (status transition to "started").
     * Maps to {@link AuditAction#UPDATE} on the {@code BATCH} aggregate.
     */
    @EventListener(BatchStartedIntegrationEvent.class)
    public void on(BatchStartedIntegrationEvent event) {
        log.info("RA received batch started event: {}", event);

        auditLogCommandService.handle(new RecordAuditLogEntryCommand(
                AuditAction.UPDATE,
                "BATCH",
                event.batchId(),
                currentUser.userId(),
                event.toString()
        ));
    }

    /**
     * Records the release of a batch. The signed release is material to the audit trail
     * (batch traceability), so it is logged as an {@link AuditAction#UPDATE} on {@code BATCH}.
     */
    @EventListener(BatchReleasedIntegrationEvent.class)
    public void on(BatchReleasedIntegrationEvent event) {
        log.info("RA received batch released event: {}", event);

        auditLogCommandService.handle(new RecordAuditLogEntryCommand(
                AuditAction.UPDATE,
                "BATCH",
                event.batchId(),
                currentUser.userId(),
                event.toString()
        ));
    }

    /**
     * Records the rejection of a batch, including the rejection reason (carried by the event).
     * Maps to {@link AuditAction#UPDATE} on {@code BATCH}.
     */
    @EventListener(BatchRejectedIntegrationEvent.class)
    public void on(BatchRejectedIntegrationEvent event) {
        log.info("RA received batch rejected event: {}", event);

        auditLogCommandService.handle(new RecordAuditLogEntryCommand(
                AuditAction.UPDATE,
                "BATCH",
                event.batchId(),
                currentUser.userId(),
                event.toString()
        ));
    }

    /**
     * Records the link between a raw material lot and a batch.
     *
     * <p>Uses a dedicated aggregate type {@code RAW_MATERIAL_USAGE} (instead of {@code BATCH})
     * because the audited fact is the usage link, not the batch itself. The {@code entityId}
     * still points to the batch, since the usage has no stable id of its own in the event.
     */
    @EventListener(RawMaterialLinkedToBatchIntegrationEvent.class)
    public void on(RawMaterialLinkedToBatchIntegrationEvent event) {
        log.info("RA received raw material linked to batch event: {}", event);

        auditLogCommandService.handle(new RecordAuditLogEntryCommand(
                AuditAction.REGISTER,
                "RAW_MATERIAL_USAGE",
                event.batchId(),
                currentUser.userId(),
                event.toString()
        ));
    }

    /**
     * Records the creation of a pharmaceutical product.
     * Audited on its own {@code PHARMACEUTICAL_PRODUCT} aggregate, using {@code productId}
     * as the entity id.
     */
    @EventListener(ProductCreatedIntegrationEvent.class)
    public void on(ProductCreatedIntegrationEvent event) {
        log.info("RA received product created event: {}", event);
        auditLogCommandService.handle(new RecordAuditLogEntryCommand(
                AuditAction.REGISTER,
                "PHARMACEUTICAL_PRODUCT",
                event.productId(),
                currentUser.userId(),
                event.toString()
        ));
    }

    /**
     * Records the registration of a staff or equipment participation in a batch.
     * Audited on the {@code BATCH} aggregate because the participation is a property
     * of the batch rather than an independent entity.
     */
    @EventListener(BatchParticipationRegisteredIntegrationEvent.class)
    public void on(BatchParticipationRegisteredIntegrationEvent event) {
        log.info("RA received batch participation event: {}", event);
        auditLogCommandService.handle(new RecordAuditLogEntryCommand(
                AuditAction.REGISTER,
                "BATCH",
                event.batchId(),
                currentUser.userId(),
                event.toString()
        ));
    }

    /**
     * Records the assignment of a batch to a monitored container.
     * Maps to {@link AuditAction#UPDATE} on {@code BATCH} because the storage assignment
     * is a state change of the batch, not a new entity.
     */
    @EventListener(BatchStoredInContainerIntegrationEvent.class)
    public void on(BatchStoredInContainerIntegrationEvent event) {
        log.info("RA received batch storage event: {}", event);
        auditLogCommandService.handle(new RecordAuditLogEntryCommand(
                AuditAction.UPDATE,
                "BATCH",
                event.batchId(),
                currentUser.userId(),
                event.toString()
        ));
    }
}
