package com.iotech.qualitrack.platform.ca.application.internal.eventhandlers;

import com.iotech.qualitrack.platform.ca.domain.model.entities.ComplianceEvent;
import com.iotech.qualitrack.platform.ca.domain.model.valueobjects.ComplianceEventType;
import com.iotech.qualitrack.platform.ca.domain.repositories.ComplianceEventRepository;
import com.iotech.qualitrack.platform.equipment.interfaces.events.CalibrationExpiredIntegrationEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Handles equipment calibration expiration integration events by recording a compliance event of the equipment.
 *
 * <p>Deviation alerts belong to the deviations of an environment or of a monitored container; an expired
 * calibration is kept as compliance evidence of the equipment.</p>
 */
@Service
@Slf4j
public class CalibrationExpiredEventHandler {

    private final ComplianceEventRepository complianceEventRepository;

    /**
     * Creates a new CalibrationExpiredEventHandler.
     *
     * @param complianceEventRepository compliance event repository
     */
    public CalibrationExpiredEventHandler(ComplianceEventRepository complianceEventRepository) {
        this.complianceEventRepository = complianceEventRepository;
    }

    /**
     * Handles the published CalibrationExpiredIntegrationEvent from the Equipment bounded context.
     *
     * @param event the calibration expired integration event
     */
    @EventListener(CalibrationExpiredIntegrationEvent.class)
    public void on(CalibrationExpiredIntegrationEvent event) {
        log.warn(
                "CA received calibration expired integration event for equipment ID '{}'.",
                event.equipmentId()
        );

        complianceEventRepository.save(new ComplianceEvent(
                event.equipmentId(),
                ComplianceEventType.EQUIPMENT_CALIBRATION_EXPIRED,
                "Calibration of equipment '%s' (%s) expired.".formatted(event.equipmentName(), event.serialNumber()),
                Instant.now().toString(),
                null
        ));
    }
}
