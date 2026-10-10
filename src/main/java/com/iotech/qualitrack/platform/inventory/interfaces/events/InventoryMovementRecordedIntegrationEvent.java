package com.iotech.qualitrack.platform.inventory.interfaces.events;

import java.time.Instant;

/**
 * Published for every movement of a raw material lot: RECEIPT, REVIEW or CONSUMPTION.
 *
 * @param actorId user who registered the movement
 */
public record InventoryMovementRecordedIntegrationEvent(Long movementId, Long laboratoryId, Long rawMaterialId,
        Long receiptId, String type, String statusAfter, Long actorId, Instant occurredAt) { }
