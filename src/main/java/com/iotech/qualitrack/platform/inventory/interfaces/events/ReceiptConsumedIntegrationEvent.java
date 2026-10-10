package com.iotech.qualitrack.platform.inventory.interfaces.events;

import java.math.BigDecimal;
import java.time.Instant;

public record ReceiptConsumedIntegrationEvent(Long receiptId, Long materialId, String materialName,
        Long productBatchId, BigDecimal amount, String unit, BigDecimal stockBefore,
        BigDecimal stockAfter, Instant occurredAt, String operationId) { }
