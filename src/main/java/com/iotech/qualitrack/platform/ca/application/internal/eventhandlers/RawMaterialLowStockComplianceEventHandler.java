package com.iotech.qualitrack.platform.ca.application.internal.eventhandlers;

import com.iotech.qualitrack.platform.ca.domain.model.entities.ComplianceEvent;
import com.iotech.qualitrack.platform.ca.domain.model.valueobjects.ComplianceEventType;
import com.iotech.qualitrack.platform.ca.domain.repositories.ComplianceEventRepository;
import com.iotech.qualitrack.platform.laboratory.interfaces.events.RawMaterialLowStockIntegrationEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

/**
 * Handles raw material low stock integration events by recording compliance events.
 */
@Service
@Slf4j
public class RawMaterialLowStockComplianceEventHandler {

    private final ComplianceEventRepository complianceEventRepository;
    private final Clock complianceClock;

    /**
     * Creates a new RawMaterialLowStockComplianceEventHandler.
     *
     * @param complianceEventRepository compliance event repository
     */
    public RawMaterialLowStockComplianceEventHandler(ComplianceEventRepository complianceEventRepository, Clock complianceClock) {
        this.complianceEventRepository = complianceEventRepository;
        this.complianceClock = complianceClock;
    }

    /**
     * Handles the published RawMaterialLowStockIntegrationEvent from the Laboratory bounded context.
     *
     * @param event the raw material low stock integration event
     */
    @EventListener(RawMaterialLowStockIntegrationEvent.class)
    public void on(RawMaterialLowStockIntegrationEvent event) {
        log.warn(
                "CA received raw material low stock integration event. rawMaterialId={}, laboratoryId={}, materialName={}",
                event.rawMaterialId(),
                event.laboratoryId(),
                event.materialName()
        );

        complianceEventRepository.save(new ComplianceEvent(
                event.rawMaterialId(),
                ComplianceEventType.RAW_MATERIAL_LOW_STOCK,
                "Raw material '%s' from laboratory ID %d is below minimum stock. Current stock: %s, minimum threshold: %s."
                        .formatted(
                                event.materialName(),
                                event.laboratoryId(),
                                event.currentStock(),
                                event.minimumThreshold()
                        ),
                Instant.now(complianceClock).toString(),
                null
        ));
    }
}
