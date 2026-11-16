package com.iotech.qualitrack.platform.ca.application.internal.eventhandlers;

import com.iotech.qualitrack.platform.batch.interfaces.events.BatchRejectedIntegrationEvent;
import com.iotech.qualitrack.platform.ca.domain.model.entities.ComplianceEvent;
import com.iotech.qualitrack.platform.ca.domain.model.valueobjects.ComplianceEventType;
import com.iotech.qualitrack.platform.ca.domain.repositories.ComplianceEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

/**
 * Handles batch rejection integration events by recording compliance audit events.
 */
@Service
@Slf4j
public class BatchRejectedComplianceEventHandler {

    private final ComplianceEventRepository complianceEventRepository;
    private final Clock complianceClock;

    /**
     * Creates a new BatchRejectedComplianceEventHandler.
     *
     * @param complianceEventRepository compliance event repository
     */
    public BatchRejectedComplianceEventHandler(ComplianceEventRepository complianceEventRepository, Clock complianceClock) {
        this.complianceEventRepository = complianceEventRepository;
        this.complianceClock = complianceClock;
    }

    /**
     * Handles the published BatchRejectedIntegrationEvent from the Batch bounded context.
     *
     * @param event the batch rejected integration event
     */
    @EventListener(BatchRejectedIntegrationEvent.class)
    public void on(BatchRejectedIntegrationEvent event) {
        log.warn("CA received batch rejected integration event for batch ID '{}'.", event.batchId());

        complianceEventRepository.save(new ComplianceEvent(
                event.batchId(),
                ComplianceEventType.BATCH_REJECTED,
                "Batch '%s' was rejected. Reason: %s.".formatted(event.batchNumber(), event.reason()),
                Instant.now(complianceClock).toString(),
                null
        ));
    }
}
