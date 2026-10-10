package com.iotech.qualitrack.platform.ra.application.internal.eventhandlers;

import com.iotech.qualitrack.platform.equipment.interfaces.events.BpmParameterConfiguredIntegrationEvent;
import com.iotech.qualitrack.platform.equipment.interfaces.events.CalibrationExpiredIntegrationEvent;
import com.iotech.qualitrack.platform.equipment.interfaces.events.EquipmentAssignedToEnvironmentIntegrationEvent;
import com.iotech.qualitrack.platform.equipment.interfaces.events.EquipmentRegisteredIntegrationEvent;
import com.iotech.qualitrack.platform.equipment.interfaces.events.EquipmentStatusChangedIntegrationEvent;
import com.iotech.qualitrack.platform.equipment.interfaces.events.MaintenanceRegisteredIntegrationEvent;
import com.iotech.qualitrack.platform.equipment.interfaces.events.SensorLinkedIntegrationEvent;
import com.iotech.qualitrack.platform.ra.domain.model.valueobjects.AuditAction;
import com.iotech.qualitrack.platform.ra.interfaces.acl.RaContextFacade;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/**
 * Handles Equipment integration events and records audit logs in RA.
 */
@Service
@Slf4j
public class EquipmentAuditEventHandler {

    private final com.iotech.qualitrack.platform.shared.application.security.CurrentUser currentUser;

    private final RaContextFacade raContextFacade;

    public EquipmentAuditEventHandler(RaContextFacade raContextFacade,
            com.iotech.qualitrack.platform.shared.application.security.CurrentUser currentUser) {
        this.currentUser = currentUser;
        this.raContextFacade = raContextFacade;
    }

    @EventListener(EquipmentRegisteredIntegrationEvent.class)
    public void on(EquipmentRegisteredIntegrationEvent event) {
        log.info("RA received equipment registered event: {}", event);

        raContextFacade.recordAuditLog(
                AuditAction.REGISTER,
                "EQUIPMENT",
                event.equipmentId(),
                currentUser.userId(),
                event.toString()
        );
    }

    @EventListener(BpmParameterConfiguredIntegrationEvent.class)
    public void on(BpmParameterConfiguredIntegrationEvent event) {
        log.info("RA received BPM parameter configured event: {}", event);

        raContextFacade.recordAuditLog(
                AuditAction.UPDATE,
                "BPM_PARAMETER_CONFIG",
                event.configId(),
                currentUser.userId(),
                event.toString()
        );
    }

    @EventListener(MaintenanceRegisteredIntegrationEvent.class)
    public void on(MaintenanceRegisteredIntegrationEvent event) {
        log.info("RA received maintenance registered event: {}", event);

        raContextFacade.recordAuditLog(
                AuditAction.REGISTER,
                "MAINTENANCE_RECORD",
                event.maintenanceRecordId(),
                currentUser.userId(),
                event.toString()
        );
    }

    @EventListener(CalibrationExpiredIntegrationEvent.class)
    public void on(CalibrationExpiredIntegrationEvent event) {
        log.info("RA received calibration expired event: {}", event);

        raContextFacade.recordAuditLog(
                AuditAction.UPDATE,
                "EQUIPMENT",
                event.equipmentId(),
                currentUser.userId(),
                event.toString()
        );
    }

    @EventListener(SensorLinkedIntegrationEvent.class)
    public void on(SensorLinkedIntegrationEvent event) {
        log.info("RA received sensor linked event: {}", event);

        raContextFacade.recordAuditLog(
                AuditAction.UPDATE,
                "EQUIPMENT",
                event.equipmentId(),
                currentUser.userId(),
                event.toString()
        );
    }

    /**
     * Records in the audit trail that an equipment or IoT device was located in an environment.
     *
     * @param event the equipment location integration event
     */
    @EventListener(EquipmentAssignedToEnvironmentIntegrationEvent.class)
    public void on(EquipmentAssignedToEnvironmentIntegrationEvent event) {
        log.info("RA received equipment located event: {}", event);
        raContextFacade.recordAuditLog(
                AuditAction.UPDATE,
                "EQUIPMENT",
                event.equipmentId(),
                currentUser.userId(),
                event.toString()
        );
    }

    /**
     * Records in the audit trail a change in the operational status of an equipment.
     *
     * @param event the status change integration event
     */
    @EventListener(EquipmentStatusChangedIntegrationEvent.class)
    public void on(EquipmentStatusChangedIntegrationEvent event) {
        log.info("RA received equipment status changed event: {}", event);
        raContextFacade.recordAuditLog(
                AuditAction.UPDATE,
                "EQUIPMENT",
                event.equipmentId(),
                event.changedByUserId(),
                event.toString()
        );
    }
}
