package com.iotech.qualitrack.platform.equipment.application.internal.commandservices;

import com.iotech.qualitrack.platform.equipment.application.commandservices.EquipmentCommandService;
import com.iotech.qualitrack.platform.equipment.application.internal.outboundservices.acl.ExternalLabService;
import com.iotech.qualitrack.platform.equipment.domain.model.aggregates.Equipment;
import com.iotech.qualitrack.platform.equipment.domain.model.commands.AssignEquipmentToEnvironmentCommand;
import com.iotech.qualitrack.platform.equipment.domain.model.commands.ChangeEquipmentStatusCommand;
import com.iotech.qualitrack.platform.equipment.domain.model.commands.LinkSensorCommand;
import com.iotech.qualitrack.platform.equipment.domain.model.commands.RegisterEquipmentCommand;
import com.iotech.qualitrack.platform.equipment.domain.model.commands.RegisterIotDeviceCommand;
import com.iotech.qualitrack.platform.equipment.domain.model.entities.EquipmentStatusChange;
import com.iotech.qualitrack.platform.equipment.domain.model.events.EquipmentAssignedToEnvironmentEvent;
import com.iotech.qualitrack.platform.equipment.domain.model.events.EquipmentRegisteredEvent;
import com.iotech.qualitrack.platform.equipment.domain.model.events.EquipmentStatusChangedEvent;
import com.iotech.qualitrack.platform.equipment.domain.model.events.SensorLinkedEvent;
import com.iotech.qualitrack.platform.equipment.domain.model.valueobjects.DeviceId;
import com.iotech.qualitrack.platform.equipment.domain.model.valueobjects.IotDeviceType;
import com.iotech.qualitrack.platform.equipment.domain.repositories.EquipmentRepository;
import com.iotech.qualitrack.platform.equipment.domain.repositories.EquipmentStatusChangeRepository;
import com.iotech.qualitrack.platform.shared.application.result.ApplicationError;
import com.iotech.qualitrack.platform.shared.application.result.Result;
import com.iotech.qualitrack.platform.shared.application.security.CurrentUser;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Objects;

/**
 * Application service implementation that executes equipment commands.
 *
 * <p>Registers equipment and IoT devices, locates them in environments and keeps the history of their
 * operational status, enforcing laboratory ownership, unique device identities and one environmental
 * device per environment.</p>
 */
@Service
public class EquipmentCommandServiceImpl implements EquipmentCommandService {

    private final EquipmentRepository equipmentRepository;
    private final EquipmentStatusChangeRepository statusChangeRepository;
    private final ExternalLabService externalLabService;
    private final CurrentUser currentUser;
    private final Clock clock;
    private final ApplicationEventPublisher eventPublisher;

    public EquipmentCommandServiceImpl(EquipmentRepository equipmentRepository,
                                       EquipmentStatusChangeRepository statusChangeRepository,
                                       ExternalLabService externalLabService,
                                       CurrentUser currentUser,
                                       Clock equipmentClock,
                                       ApplicationEventPublisher eventPublisher) {
        this.equipmentRepository = equipmentRepository;
        this.statusChangeRepository = statusChangeRepository;
        this.externalLabService = externalLabService;
        this.currentUser = currentUser;
        this.clock = equipmentClock;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public Result<Equipment, ApplicationError> handle(RegisterEquipmentCommand command) {
        if (!externalLabService.existsLaboratoryById(command.labId())) {
            return Result.failure(ApplicationError.notFound("Laboratory", command.labId()));
        }
        if (equipmentRepository.existsBySerialNumber(command.serialNumber())) {
            return Result.failure(serialNumberConflict(command.serialNumber()));
        }
        var equipment = equipmentRepository.save(new Equipment(command));
        eventPublisher.publishEvent(EquipmentRegisteredEvent.from(equipment));
        return Result.success(equipment);
    }

    @Override
    @Transactional
    public Result<Equipment, ApplicationError> handle(RegisterIotDeviceCommand command) {
        if (!externalLabService.existsLaboratoryById(command.laboratoryId())) {
            return Result.failure(ApplicationError.notFound("Laboratory", command.laboratoryId()));
        }
        if (equipmentRepository.existsBySensorExternalId(new DeviceId(command.sensorExternalId()))) {
            return Result.failure(ApplicationError.conflict(resourceName(command.deviceType()),
                    "Device with identifier '%s' already exists".formatted(command.sensorExternalId())));
        }
        if (equipmentRepository.existsBySerialNumber(command.serialNumber())) {
            return Result.failure(serialNumberConflict(command.serialNumber()));
        }
        var device = equipmentRepository.save(new Equipment(command));
        eventPublisher.publishEvent(EquipmentRegisteredEvent.from(device));
        return Result.success(device);
    }

    @Override
    @Transactional
    public Result<Equipment, ApplicationError> handle(AssignEquipmentToEnvironmentCommand command) {
        var required = command.requiredDeviceType();
        var found = equipmentRepository.findById(command.equipmentId())
                .filter(item -> item.belongsTo(command.laboratoryId()))
                .filter(item -> required == null || item.isDeviceOfType(required));
        if (found.isEmpty()) {
            return Result.failure(ApplicationError.notFound(resourceName(required), command.equipmentId()));
        }
        if (!externalLabService.existsEnvironment(command.laboratoryId(), command.environmentId())) {
            return Result.failure(ApplicationError.notFound("Environment", command.environmentId()));
        }
        var equipment = found.get();
        if (equipment.isDeviceOfType(IotDeviceType.ENVIRONMENTAL_DEVICE) && equipmentRepository.existsOtherDeviceInEnvironment(
                command.environmentId(), IotDeviceType.ENVIRONMENTAL_DEVICE, equipment.getId())) {
            return Result.failure(ApplicationError.conflict("Environment",
                    "Environment %d already has an environmental device".formatted(command.environmentId())));
        }
        var previousEnvironmentId = equipment.getEnvironmentId();
        equipment.assignToEnvironment(command.environmentId());
        var located = equipmentRepository.save(equipment);
        if (!Objects.equals(previousEnvironmentId, located.getEnvironmentId())) {
            eventPublisher.publishEvent(EquipmentAssignedToEnvironmentEvent.from(located, previousEnvironmentId));
        }
        return Result.success(located);
    }

    @Override
    @Transactional
    public Result<EquipmentStatusChange, ApplicationError> handle(ChangeEquipmentStatusCommand command) {
        var found = equipmentRepository.findById(command.equipmentId())
                .filter(item -> item.isLocatedIn(command.laboratoryId(), command.environmentId()));
        if (found.isEmpty()) {
            return Result.failure(ApplicationError.notFound("Equipment", command.equipmentId()));
        }
        var equipment = found.get();
        EquipmentStatusChange change;
        try {
            change = equipment.changeStatus(command.status(), command.reason(), currentUser.userId(), clock.instant());
        } catch (IllegalArgumentException e) {
            return Result.failure(ApplicationError.validationError("status", e.getMessage()));
        }
        equipmentRepository.save(equipment);
        var recorded = statusChangeRepository.save(change);
        eventPublisher.publishEvent(EquipmentStatusChangedEvent.from(equipment, recorded));
        return Result.success(recorded);
    }

    @Override
    public Result<Long, ApplicationError> handle(LinkSensorCommand command) {
        var result = equipmentRepository.findById(command.equipmentId());

        if (result.isEmpty()) {
            return Result.failure(ApplicationError.notFound(
                    "Equipment",
                    String.valueOf(command.equipmentId())
            ));
        }

        var equipment = result.get();

        try {
            equipment.linkSensor(command.sensorExternalId());
            var updatedEquipment = equipmentRepository.save(equipment);

            eventPublisher.publishEvent(new SensorLinkedEvent(
                    updatedEquipment.getId(),
                    command.sensorExternalId()
            ));

            return Result.success(updatedEquipment.getId());

        } catch (IllegalArgumentException e) {
            return Result.failure(ApplicationError.validationError("Equipment", e.getMessage()));
        } catch (Exception e) {
            return Result.failure(ApplicationError.unexpected("link-sensor", e.getMessage()));
        }
    }

    private static ApplicationError serialNumberConflict(String serialNumber) {
        return ApplicationError.conflict("Equipment",
                "Equipment with serial number '%s' already exists".formatted(serialNumber));
    }

    private static String resourceName(IotDeviceType deviceType) {
        if (deviceType == null) return "Equipment";
        return switch (deviceType) {
            case ENVIRONMENTAL_DEVICE -> "EnvironmentalDevice";
            case CONTAINER_MONITOR -> "ContainerMonitor";
        };
    }
}
