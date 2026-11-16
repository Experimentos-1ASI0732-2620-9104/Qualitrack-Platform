package com.iotech.qualitrack.platform.ca.application.internal.eventhandlers;

import com.iotech.qualitrack.platform.batch.interfaces.events.BatchReleasedIntegrationEvent;
import com.iotech.qualitrack.platform.ca.domain.model.entities.ComplianceEvent;
import com.iotech.qualitrack.platform.ca.domain.model.valueobjects.ComplianceEventType;
import com.iotech.qualitrack.platform.ca.domain.repositories.ComplianceEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

/**
 * Handles batch release integration events by recording compliance audit events.
 */
@Service
@Slf4j
public class BatchReleasedComplianceEventHandler {

    private final ComplianceEventRepository complianceEventRepository;
    private final Clock complianceClock;

    /**
     * Creates a new BatchReleasedComplianceEventHandler.
     *
     * @param complianceEventRepository compliance event repository
     */
    public BatchReleasedComplianceEventHandler(ComplianceEventRepository complianceEventRepository, Clock complianceClock) {
        this.complianceEventRepository = complianceEventRepository;
        this.complianceClock = complianceClock;
    }

    /**
     * Handles the published BatchReleasedIntegrationEvent from the Batch bounded context.
     *
     * @param event the batch released integration event
     */
    @EventListener(BatchReleasedIntegrationEvent.class)
    public void on(BatchReleasedIntegrationEvent event) {
        log.info("CA received batch released integration event for batch ID '{}'.", event.batchId());

        complianceEventRepository.save(new ComplianceEvent(
                event.batchId(),
                ComplianceEventType.BATCH_RELEASED,
                "Batch '%s' was released on %s.".formatted(event.batchNumber(), event.releaseDate()),
                Instant.now(complianceClock).toString(),
                null
        ));
    }
}
