package com.iotech.qualitrack.platform.ra.application.internal.eventhandlers;

import com.iotech.qualitrack.platform.laboratory.interfaces.events.EnvironmentRegisteredIntegrationEvent;
import com.iotech.qualitrack.platform.laboratory.interfaces.events.EnvironmentUpdatedIntegrationEvent;
import com.iotech.qualitrack.platform.laboratory.interfaces.events.EnvironmentUsageAssignedIntegrationEvent;
import com.iotech.qualitrack.platform.laboratory.interfaces.events.LaboratoryRegisteredIntegrationEvent;
import com.iotech.qualitrack.platform.laboratory.interfaces.events.RawMaterialCreatedIntegrationEvent;
import com.iotech.qualitrack.platform.laboratory.interfaces.events.RawMaterialLowStockIntegrationEvent;
import com.iotech.qualitrack.platform.laboratory.interfaces.events.StaffDeactivatedIntegrationEvent;
import com.iotech.qualitrack.platform.laboratory.interfaces.events.StaffRegisteredIntegrationEvent;
import com.iotech.qualitrack.platform.ra.domain.model.valueobjects.AuditAction;
import com.iotech.qualitrack.platform.ra.application.commandservices.AuditLogCommandService;
import com.iotech.qualitrack.platform.ra.domain.model.commands.RecordAuditLogEntryCommand;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/**
 * Handles Laboratory integration events and records audit logs in RA.
 */
@Service
@Slf4j
public class LaboratoryAuditEventHandler {

    private final com.iotech.qualitrack.platform.shared.application.security.CurrentUser currentUser;

    private final AuditLogCommandService auditLogCommandService;

    public LaboratoryAuditEventHandler(AuditLogCommandService auditLogCommandService,
            com.iotech.qualitrack.platform.shared.application.security.CurrentUser currentUser) {
        this.currentUser = currentUser;
        this.auditLogCommandService = auditLogCommandService;
    }

    /**
     * Records the registration of a laboratory.
     * Maps to {@link AuditAction#REGISTER} on the {@code LABORATORY} aggregate.
     */
    @EventListener(LaboratoryRegisteredIntegrationEvent.class)
    public void on(LaboratoryRegisteredIntegrationEvent event) {
        log.info("RA received laboratory registered event: {}", event);

        auditLogCommandService.handle(new RecordAuditLogEntryCommand(
                AuditAction.REGISTER,
                "LABORATORY",
                event.laboratoryId(),
                currentUser.userId(),
                event.toString()
        ));
    }

    /**
     * Records the creation of a raw material in the laboratory catalogue.
     * Maps to {@link AuditAction#REGISTER} on the {@code RAW_MATERIAL} aggregate.
     *
     * <p>Note: this event covers creation only. For updates to an existing raw material,
     * the Inventory context publishes {@code RawMaterialSavedIntegrationEvent}, which is
     * consumed separately by {@code InventoryAuditEventHandler}. Both handlers intentionally
     * target the same aggregate type.
     */
    @EventListener(RawMaterialCreatedIntegrationEvent.class)
    public void on(RawMaterialCreatedIntegrationEvent event) {
        log.info("RA received raw material created event: {}", event);

        auditLogCommandService.handle(new RecordAuditLogEntryCommand(
                AuditAction.REGISTER,
                "RAW_MATERIAL",
                event.rawMaterialId(),
                currentUser.userId(),
                event.toString()
        ));
    }

    /**
     * Records the registration of a staff member.
     * Maps to {@link AuditAction#REGISTER} on the {@code STAFF_MEMBER} aggregate.
     */
    @EventListener(StaffRegisteredIntegrationEvent.class)
    public void on(StaffRegisteredIntegrationEvent event) {
        log.info("RA received staff registered event: {}", event);

        auditLogCommandService.handle(new RecordAuditLogEntryCommand(
                AuditAction.REGISTER,
                "STAFF_MEMBER",
                event.staffMemberId(),
                currentUser.userId(),
                event.toString()
        ));
    }

    /**
     * Records the deactivation of a staff member.
     * Maps to {@link AuditAction#UPDATE} on the {@code STAFF_MEMBER} aggregate.
     */
    @EventListener(StaffDeactivatedIntegrationEvent.class)
    public void on(StaffDeactivatedIntegrationEvent event) {
        log.info("RA received staff deactivated event: {}", event);

        auditLogCommandService.handle(new RecordAuditLogEntryCommand(
                AuditAction.UPDATE,
                "STAFF_MEMBER",
                event.staffMemberId(),
                currentUser.userId(),
                event.toString()
        ));
    }

    /**
     * Records a low-stock notification for a raw material.
     *
     * <p>Mapped to {@link AuditAction#UPDATE} on the {@code RAW_MATERIAL} aggregate because
     * the notification reflects a change of operational state, not a new catalogue entry.
     * May fire multiple times for the same material if it crosses the threshold repeatedly.
     */

    @EventListener(RawMaterialLowStockIntegrationEvent.class)
    public void on(RawMaterialLowStockIntegrationEvent event) {
        log.info("RA received raw material low stock event: {}", event);

        auditLogCommandService.handle(new RecordAuditLogEntryCommand(
                AuditAction.UPDATE,
                "RAW_MATERIAL",
                event.rawMaterialId(),
                currentUser.userId(),
                event.toString()
        ));
    }

    /**
     * Records the registration of an environment within the laboratory.
     * Maps to {@link AuditAction#REGISTER} on the {@code ENVIRONMENT} aggregate.
     */
    @EventListener(EnvironmentRegisteredIntegrationEvent.class)
    public void on(EnvironmentRegisteredIntegrationEvent event) {
        log.info("RA received environment registered event: {}", event);

        auditLogCommandService.handle(new RecordAuditLogEntryCommand(
                AuditAction.REGISTER,
                "ENVIRONMENT",
                event.environmentId(),
                currentUser.userId(),
                event.toString()
        ));
    }

    /**
     * Records an update to an environment (name, capacity, type, etc.).
     * Maps to {@link AuditAction#UPDATE} on the {@code ENVIRONMENT} aggregate.
     */
    @EventListener(EnvironmentUpdatedIntegrationEvent.class)
    public void on(EnvironmentUpdatedIntegrationEvent event) {
        log.info("RA received environment updated event: {}", event);

        auditLogCommandService.handle(new RecordAuditLogEntryCommand(
                AuditAction.UPDATE,
                "ENVIRONMENT",
                event.environmentId(),
                currentUser.userId(),
                event.toString()
        ));
    }

    /**
     * Records the assignment of a usage to an environment.
     *
     * <p>Unlike the other handlers in this class, the actor is taken from the event payload
     * ({@code assignedBy}) rather than the security context. Usage assignments can be
     * triggered by background reconfigurations where the HTTP security context is not
     * available; the event payload is the reliable source of the actor in that case.
     * Maps to {@link AuditAction#UPDATE} on the {@code ENVIRONMENT} aggregate.
     */
    @EventListener(EnvironmentUsageAssignedIntegrationEvent.class)
    public void on(EnvironmentUsageAssignedIntegrationEvent event) {
        log.info("RA received environment usage assigned event: {}", event);

        auditLogCommandService.handle(new RecordAuditLogEntryCommand(
                AuditAction.UPDATE,
                "ENVIRONMENT",
                event.environmentId(),
                event.assignedBy(),
                event.toString()
        ));
    }
}
