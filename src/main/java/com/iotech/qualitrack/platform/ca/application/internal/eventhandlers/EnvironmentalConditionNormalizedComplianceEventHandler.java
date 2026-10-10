package com.iotech.qualitrack.platform.ca.application.internal.eventhandlers;

import com.iotech.qualitrack.platform.ca.application.commandservices.CaCommandService;
import com.iotech.qualitrack.platform.ca.domain.model.commands.RecordConditionNormalizedCommand;
import com.iotech.qualitrack.platform.tracking.interfaces.events.EnvironmentalConditionNormalizedIntegrationEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/**
 * Notes in the open alert of an incident that its metric returned to normal, as reported by Tracking &amp; Telemetry.
 */
@Service
@Slf4j
public class EnvironmentalConditionNormalizedComplianceEventHandler {
    private final CaCommandService caCommandService;

    public EnvironmentalConditionNormalizedComplianceEventHandler(CaCommandService caCommandService) {
        this.caCommandService = caCommandService;
    }

    @EventListener(EnvironmentalConditionNormalizedIntegrationEvent.class)
    public void on(EnvironmentalConditionNormalizedIntegrationEvent event) {
        caCommandService.handle(new RecordConditionNormalizedCommand(event.laboratoryId(), event.deviceId(), event.metric(),
                event.measuredAt())).ifPresent(alert -> log.info("Alert {} noted that {} is back to normal",
                alert.getId(), alert.getParameterName()));
    }
}
