package com.iotech.qualitrack.platform.equipment.application.internal.eventhandlers;

import com.iotech.qualitrack.platform.equipment.domain.model.events.EquipmentAssignedToEnvironmentEvent;
import com.iotech.qualitrack.platform.equipment.domain.model.events.EquipmentStatusChangedEvent;
import com.iotech.qualitrack.platform.equipment.interfaces.events.EquipmentAssignedToEnvironmentIntegrationEvent;
import com.iotech.qualitrack.platform.equipment.interfaces.events.EquipmentStatusChangedIntegrationEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/**
 * Publishes the location and operational status changes of equipment as Equipment integration events.
 */
@Service
@Slf4j
public class EquipmentLocationEventHandler {
    private final ApplicationEventPublisher eventPublisher;

    public EquipmentLocationEventHandler(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @EventListener(EquipmentAssignedToEnvironmentEvent.class)
    public void on(EquipmentAssignedToEnvironmentEvent event) {
        log.info("Equipment located. equipmentId={}, laboratoryId={}, environmentId={}, previousEnvironmentId={}",
                event.equipmentId(), event.laboratoryId(), event.environmentId(), event.previousEnvironmentId());
        eventPublisher.publishEvent(EquipmentAssignedToEnvironmentIntegrationEvent.from(event));
    }

    @EventListener(EquipmentStatusChangedEvent.class)
    public void on(EquipmentStatusChangedEvent event) {
        log.info("Equipment status changed. equipmentId={}, from={}, to={}",
                event.equipmentId(), event.previousStatus(), event.newStatus());
        eventPublisher.publishEvent(EquipmentStatusChangedIntegrationEvent.from(event));
    }
}
