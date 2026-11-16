package com.iotech.qualitrack.platform.ra.application.internal.eventhandlers;

import com.iotech.qualitrack.platform.inventory.interfaces.events.InventoryMovementRecordedIntegrationEvent;
import com.iotech.qualitrack.platform.inventory.interfaces.events.RawMaterialSavedIntegrationEvent;
import com.iotech.qualitrack.platform.ra.domain.model.valueobjects.AuditAction;
import com.iotech.qualitrack.platform.ra.application.commandservices.AuditLogCommandService;
import com.iotech.qualitrack.platform.ra.domain.model.commands.RecordAuditLogEntryCommand;
import com.iotech.qualitrack.platform.shared.application.security.CurrentUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/**
 * Records in the audit trail the inventory catalogue changes and the receipts and reviews of raw material lots.
 * Consumptions are audited as raw material usages of the product batch.
 */
@Service
@Slf4j
public class InventoryAuditEventHandler {
    private final AuditLogCommandService auditLogCommandService;
    private final CurrentUser currentUser;

    public InventoryAuditEventHandler(AuditLogCommandService auditLogCommandService, CurrentUser currentUser) {
        this.auditLogCommandService = auditLogCommandService;
        this.currentUser = currentUser;
    }

    /**
     * Records a creation or update of a raw material in the catalogue.
     *
     * <p>The event carries a {@code created} flag; it is mapped to
     * {@link AuditAction#REGISTER} on creation and {@link AuditAction#UPDATE} otherwise,
     * so a single handler covers both cases while preserving the correct semantic action
     * in the audit trail.
     */
    @EventListener(RawMaterialSavedIntegrationEvent.class)
    public void on(RawMaterialSavedIntegrationEvent event) {
        log.info("RA received raw material saved event: {}", event);
        auditLogCommandService.handle(new RecordAuditLogEntryCommand(event.created() ? AuditAction.REGISTER : AuditAction.UPDATE, "RAW_MATERIAL",
                event.rawMaterialId(), currentUser.userId(), event.toString()));
    }

    /**
     * Records receipts and reviews of raw-material lots.
     *
     * <p>Consumption movements are intentionally ignored here: they are audited by the
     * Batch context as raw-material usages of the product batch, so auditing them here
     * would duplicate the fact in the audit trail. Other movement types are audited
     * under {@code RAW_MATERIAL_BATCH}, with {@link AuditAction#REGISTER} for receipts
     * and {@link AuditAction#UPDATE} for reviews.
     */
    @EventListener(InventoryMovementRecordedIntegrationEvent.class)
    public void on(InventoryMovementRecordedIntegrationEvent event) {
        if ("CONSUMPTION".equals(event.type())) return;
        log.info("RA received inventory movement event: {}", event);
        auditLogCommandService.handle(new RecordAuditLogEntryCommand("RECEIPT".equals(event.type()) ? AuditAction.REGISTER : AuditAction.UPDATE,
                "RAW_MATERIAL_BATCH", event.receiptId(), event.actorId(), event.toString()));
    }
}
