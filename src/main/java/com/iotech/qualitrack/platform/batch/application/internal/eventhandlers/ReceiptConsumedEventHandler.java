package com.iotech.qualitrack.platform.batch.application.internal.eventhandlers;

import com.iotech.qualitrack.platform.batch.domain.model.events.BatchStartedEvent;
import com.iotech.qualitrack.platform.batch.domain.repositories.BatchRepository;
import com.iotech.qualitrack.platform.inventory.interfaces.events.ReceiptConsumedIntegrationEvent;
import com.iotech.qualitrack.platform.batch.domain.model.entities.RawMaterialUsage;
import com.iotech.qualitrack.platform.batch.domain.model.events.RawMaterialLinkedToBatchEvent;
import com.iotech.qualitrack.platform.batch.domain.repositories.RawMaterialUsageRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

/**
 * Records the raw material usage confirmed by Inventory Management and starts the batch with its first consumption.
 */
@Component
public class ReceiptConsumedEventHandler {
    private final RawMaterialUsageRepository usages;
    private final BatchRepository batches;
    private final ApplicationEventPublisher events;
    public ReceiptConsumedEventHandler(RawMaterialUsageRepository usages, BatchRepository batches, ApplicationEventPublisher events) {
        this.usages = usages; this.batches = batches; this.events = events;
    }
    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void on(ReceiptConsumedIntegrationEvent event) {
        var usage = new RawMaterialUsage(null, event.productBatchId(), event.materialId(), event.materialName(),
            event.amount().doubleValue(), event.unit(), event.occurredAt().toString());
        usage.recordStockChange(event.stockBefore(), event.stockAfter());
        usage.assignInventoryReceipt(event.receiptId());
        usage.assignOperation(event.operationId());
        events.publishEvent(RawMaterialLinkedToBatchEvent.from(usages.save(usage)));
        var batch = batches.findByIdForUpdate(event.productBatchId()).orElse(null);
        if (batch != null && batch.registerRawMaterialConsumption()) {
            events.publishEvent(BatchStartedEvent.from(batches.save(batch), event.occurredAt()));
        }
    }
}
