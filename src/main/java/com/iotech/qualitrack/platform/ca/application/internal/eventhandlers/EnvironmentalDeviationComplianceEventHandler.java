package com.iotech.qualitrack.platform.ca.application.internal.eventhandlers;

import com.iotech.qualitrack.platform.ca.application.commandservices.CaCommandService;
import com.iotech.qualitrack.platform.ca.domain.model.commands.CreateDeviationAlertCommand;
import com.iotech.qualitrack.platform.ca.domain.model.valueobjects.AlertSeverity;
import com.iotech.qualitrack.platform.tracking.interfaces.events.EnvironmentalDeviationDetectedIntegrationEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/**
 * Registers an environmental deviation detected by Tracking &amp; Telemetry, with the severity of the condition
 * (WARNING or CRITICAL), the limit crossed and its unit: it opens the alert of the incident or is added to the alert
 * that is still open for the same device and metric.
 */
@Service
@Slf4j
public class EnvironmentalDeviationComplianceEventHandler {
    private final CaCommandService caCommandService;

    public EnvironmentalDeviationComplianceEventHandler(CaCommandService caCommandService) {
        this.caCommandService = caCommandService;
    }

    @EventListener(EnvironmentalDeviationDetectedIntegrationEvent.class)
    public void on(EnvironmentalDeviationDetectedIntegrationEvent event) {
        log.warn("CA received an environmental deviation: device={}, metric={}, value={} {}, state={}, limit={}",
                event.deviceId(), event.metric(), event.value(), event.unit(), event.state(), event.thresholdValue());
        var result = caCommandService.handle(new CreateDeviationAlertCommand(
                event.laboratoryId(),
                event.environmentId(),
                event.deviceId(),
                event.measurementId(),
                event.metric(),
                event.value(),
                event.thresholdValue(),
                event.unit(),
                AlertSeverity.valueOf(event.state()),
                event.measuredAt()
        ));
        if (result.isFailure()) {
            log.error("CA could not register the environmental deviation of device {}: {}", event.deviceId(), result);
        }
    }
}
