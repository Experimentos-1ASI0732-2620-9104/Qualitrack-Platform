package com.iotech.qualitrack.platform.ra.application.internal.eventhandlers;

import com.iotech.qualitrack.platform.equipment.interfaces.events.BpmParameterConfiguredIntegrationEvent;
import com.iotech.qualitrack.platform.equipment.interfaces.events.CalibrationExpiredIntegrationEvent;
import com.iotech.qualitrack.platform.equipment.interfaces.events.EquipmentAssignedToEnvironmentIntegrationEvent;
import com.iotech.qualitrack.platform.equipment.interfaces.events.EquipmentRegisteredIntegrationEvent;
import com.iotech.qualitrack.platform.equipment.interfaces.events.EquipmentStatusChangedIntegrationEvent;
import com.iotech.qualitrack.platform.equipment.interfaces.events.MaintenanceRegisteredIntegrationEvent;
import com.iotech.qualitrack.platform.equipment.interfaces.events.SensorLinkedIntegrationEvent;
import com.iotech.qualitrack.platform.ra.domain.model.valueobjects.AuditAction;
import com.iotech.qualitrack.platform.ra.application.commandservices.AuditLogCommandService;
import com.iotech.qualitrack.platform.ra.domain.model.commands.RecordAuditLogEntryCommand;
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

    private final AuditLogCommandService auditLogCommandService;

    public EquipmentAuditEventHandler(AuditLogCommandService auditLogCommandService,
            com.iotech.qualitrack.platform.shared.application.security.CurrentUser currentUser) {
        this.currentUser = currentUser;
        this.auditLogCommandService = auditLogCommandService;
    }

    /**
     * Records the registration of an equipment.
     * Maps to {@link AuditAction#REGISTER} on the {@code EQUIPMENT} aggregate.
     */
    @EventListener(EquipmentRegisteredIntegrationEvent.class)
    public void on(EquipmentRegisteredIntegrationEvent event) {
        log.info("RA received equipment registered event: {}", event);

        auditLogCommandService.handle(new RecordAuditLogEntryCommand(
                AuditAction.REGISTER,
                "EQUIPMENT",
                event.equipmentId(),
                currentUser.userId(),
                event.toString()
        ));
    }

    /**
     * Records the configuration of a BPM parameter limit.
     * Audited on its own {@code BPM_PARAMETER_CONFIG} aggregate (identified by
     * {@code configId}) so limit changes are traceable independently of any
     * particular equipment status.
     */
    @EventListener(BpmParameterConfiguredIntegrationEvent.class)
    public void on(BpmParameterConfiguredIntegrationEvent event) {
        log.info("RA received BPM parameter configured event: {}", event);

        auditLogCommandService.handle(new RecordAuditLogEntryCommand(
                AuditAction.UPDATE,
                "BPM_PARAMETER_CONFIG",
                event.configId(),
                currentUser.userId(),
                event.toString()
        ));
    }

    /**
     * Records the registration of a maintenance intervention.
     * Audited as a dedicated {@code MAINTENANCE_RECORD} entry so maintenance history
     * can be queried per record in addition to per equipment.
     */
    @EventListener(MaintenanceRegisteredIntegrationEvent.class)
    public void on(MaintenanceRegisteredIntegrationEvent event) {
        log.info("RA received maintenance registered event: {}", event);

        auditLogCommandService.handle(new RecordAuditLogEntryCommand(
                AuditAction.REGISTER,
                "MAINTENANCE_RECORD",
                event.maintenanceRecordId(),
                currentUser.userId(),
                event.toString()
        ));
    }

    /**
     * Records the expiration of an equipment's calibration.
     * Audited on the {@code EQUIPMENT} aggregate with {@link AuditAction#UPDATE}, since
     * calibration expiry is a state change of the equipment, not a new record.
     */
    @EventListener(CalibrationExpiredIntegrationEvent.class)
    public void on(CalibrationExpiredIntegrationEvent event) {
        log.info("RA received calibration expired event: {}", event);

        auditLogCommandService.handle(new RecordAuditLogEntryCommand(
                AuditAction.UPDATE,
                "EQUIPMENT",
                event.equipmentId(),
                currentUser.userId(),
                event.toString()
        ));
    }

    /**
     * Records the link between a sensor (or IoT device) and an equipment.
     * Audited on the {@code EQUIPMENT} aggregate with {@link AuditAction#UPDATE}.
     */
    @EventListener(SensorLinkedIntegrationEvent.class)
    public void on(SensorLinkedIntegrationEvent event) {
        log.info("RA received sensor linked event: {}", event);

        auditLogCommandService.handle(new RecordAuditLogEntryCommand(
                AuditAction.UPDATE,
                "EQUIPMENT",
                event.equipmentId(),
                currentUser.userId(),
                event.toString()
        ));
    }

    /**
     * Records in the audit trail that an equipment or IoT device was located in an environment.
     *
     * @param event the equipment location integration event
     */
    @EventListener(EquipmentAssignedToEnvironmentIntegrationEvent.class)
    public void on(EquipmentAssignedToEnvironmentIntegrationEvent event) {
        log.info("RA received equipment located event: {}", event);
        auditLogCommandService.handle(new RecordAuditLogEntryCommand(
                AuditAction.UPDATE,
                "EQUIPMENT",
                event.equipmentId(),
                currentUser.userId(),
                event.toString()
        ));
    }

    /**
     * Records in the audit trail a change in the operational status of an equipment.
     *
     * @param event the status change integration event
     */
    @EventListener(EquipmentStatusChangedIntegrationEvent.class)
    public void on(EquipmentStatusChangedIntegrationEvent event) {
        log.info("RA received equipment status changed event: {}", event);
        auditLogCommandService.handle(new RecordAuditLogEntryCommand(
                AuditAction.UPDATE,
                "EQUIPMENT",
                event.equipmentId(),
                event.changedByUserId(),
                event.toString()
        ));
    }
}
