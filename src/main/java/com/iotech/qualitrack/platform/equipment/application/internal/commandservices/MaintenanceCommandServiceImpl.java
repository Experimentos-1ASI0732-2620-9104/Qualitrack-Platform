package com.iotech.qualitrack.platform.equipment.application.internal.commandservices;

import com.iotech.qualitrack.platform.equipment.application.commandservices.MaintenanceCommandService;
import com.iotech.qualitrack.platform.equipment.application.internal.outboundservices.acl.ExternalLabService;
import com.iotech.qualitrack.platform.equipment.domain.model.aggregates.MaintenanceRecord;
import com.iotech.qualitrack.platform.equipment.domain.model.commands.RegisterMaintenanceCommand;
import com.iotech.qualitrack.platform.equipment.domain.model.events.MaintenanceRegisteredEvent;
import com.iotech.qualitrack.platform.equipment.domain.repositories.EquipmentRepository;
import com.iotech.qualitrack.platform.equipment.domain.repositories.MaintenanceRepository;
import com.iotech.qualitrack.platform.shared.application.result.ApplicationError;
import com.iotech.qualitrack.platform.shared.application.result.Result;
import com.iotech.qualitrack.platform.shared.application.security.CurrentUser;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;

/**
 * Application service implementation that executes maintenance commands.
 */
@Service
public class MaintenanceCommandServiceImpl implements MaintenanceCommandService {

    private final MaintenanceRepository maintenanceRepository;
    private final EquipmentRepository equipmentRepository;
    private final Clock clock;
    private final ExternalLabService externalLabService;
    private final CurrentUser currentUser;
    private final ApplicationEventPublisher eventPublisher;

    public MaintenanceCommandServiceImpl(MaintenanceRepository maintenanceRepository,
                                         EquipmentRepository equipmentRepository,
                                         Clock equipmentClock,
                                         ExternalLabService externalLabService,
                                         CurrentUser currentUser,
                                         ApplicationEventPublisher eventPublisher) {
        this.externalLabService = externalLabService;
        this.currentUser = currentUser;
        this.maintenanceRepository = maintenanceRepository;
        this.equipmentRepository = equipmentRepository;
        this.clock = equipmentClock;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public Result<MaintenanceRecord, ApplicationError> handle(RegisterMaintenanceCommand command) {
        var located = equipmentRepository.findById(command.equipmentId())
                .filter(equipment -> equipment.isLocatedIn(command.laboratoryId(), command.environmentId()));
        if (located.isEmpty()) {
            return Result.failure(ApplicationError.notFound("Equipment", command.equipmentId()));
        }
        var technician = externalLabService.findStaffMember(command.laboratoryId(), command.technicianStaffId());
        if (technician.isEmpty()) {
            return Result.failure(ApplicationError.notFound("StaffMember", command.technicianStaffId()));
        }
        if (!technician.get().active()) {
            return Result.failure(ApplicationError.validationError("technicianStaffId", "The technician is not an active staff member"));
        }
        if (technician.get().isAuditor()) {
            return Result.failure(ApplicationError.validationError("technicianStaffId", "Auditors only consult the records and cannot perform maintenance"));
        }
        if (!currentUser.managesQuality() && !technician.get().isAccount(currentUser.userId())) {
            throw new AccessDeniedException("Staff members can only register maintenance they performed");
        }
        MaintenanceRecord maintenanceRecord;
        try {
            maintenanceRecord = new MaintenanceRecord(command, technician.get().fullName(), LocalDate.now(clock));
        } catch (IllegalArgumentException e) {
            return Result.failure(ApplicationError.validationError("MaintenanceRecord", e.getMessage()));
        }
        var savedRecord = maintenanceRepository.save(maintenanceRecord);
        eventPublisher.publishEvent(MaintenanceRegisteredEvent.from(savedRecord));
        return Result.success(savedRecord);
    }
}
