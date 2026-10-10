package com.iotech.qualitrack.platform.ra.application.internal.eventhandlers;

import com.iotech.qualitrack.platform.inventory.interfaces.events.InventoryMovementRecordedIntegrationEvent;
import com.iotech.qualitrack.platform.inventory.interfaces.events.RawMaterialSavedIntegrationEvent;
import com.iotech.qualitrack.platform.ra.domain.model.valueobjects.AuditAction;
import com.iotech.qualitrack.platform.ra.interfaces.acl.RaContextFacade;
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
    private final RaContextFacade raContextFacade;
    private final CurrentUser currentUser;

    public InventoryAuditEventHandler(RaContextFacade raContextFacade, CurrentUser currentUser) {
        this.raContextFacade = raContextFacade;
        this.currentUser = currentUser;
    }

    @EventListener(RawMaterialSavedIntegrationEvent.class)
    public void on(RawMaterialSavedIntegrationEvent event) {
        log.info("RA received raw material saved event: {}", event);
        raContextFacade.recordAuditLog(event.created() ? AuditAction.REGISTER : AuditAction.UPDATE, "RAW_MATERIAL",
                event.rawMaterialId(), currentUser.userId(), event.toString());
    }

    @EventListener(InventoryMovementRecordedIntegrationEvent.class)
    public void on(InventoryMovementRecordedIntegrationEvent event) {
        if ("CONSUMPTION".equals(event.type())) return;
        log.info("RA received inventory movement event: {}", event);
        raContextFacade.recordAuditLog("RECEIPT".equals(event.type()) ? AuditAction.REGISTER : AuditAction.UPDATE,
                "RAW_MATERIAL_BATCH", event.receiptId(), event.actorId(), event.toString());
    }
}
