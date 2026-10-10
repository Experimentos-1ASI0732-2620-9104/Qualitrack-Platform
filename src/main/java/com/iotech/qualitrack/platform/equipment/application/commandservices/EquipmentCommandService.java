package com.iotech.qualitrack.platform.equipment.application.commandservices;

import com.iotech.qualitrack.platform.equipment.domain.model.aggregates.Equipment;
import com.iotech.qualitrack.platform.equipment.domain.model.commands.AssignEquipmentToEnvironmentCommand;
import com.iotech.qualitrack.platform.equipment.domain.model.commands.ChangeEquipmentStatusCommand;
import com.iotech.qualitrack.platform.equipment.domain.model.commands.LinkSensorCommand;
import com.iotech.qualitrack.platform.equipment.domain.model.commands.RegisterEquipmentCommand;
import com.iotech.qualitrack.platform.equipment.domain.model.commands.RegisterIotDeviceCommand;
import com.iotech.qualitrack.platform.equipment.domain.model.entities.EquipmentStatusChange;
import com.iotech.qualitrack.platform.shared.application.result.ApplicationError;
import com.iotech.qualitrack.platform.shared.application.result.Result;

/**
 * Application service contract for the commands of the Equipment aggregate.
 */
public interface EquipmentCommandService {

    /**
     * Registers an equipment in a laboratory (US45).
     *
     * @return the registered equipment, NOT_FOUND for an unknown laboratory or CONFLICT for a repeated serial number
     * @see RegisterEquipmentCommand
     */
    Result<Equipment, ApplicationError> handle(RegisterEquipmentCommand command);

    /**
     * Registers an environmental device or container monitor (US51, US53).
     *
     * @return the registered device, or CONFLICT when its identity or serial number already exists
     * @see RegisterIotDeviceCommand
     */
    Result<Equipment, ApplicationError> handle(RegisterIotDeviceCommand command);

    /**
     * Locates an equipment or IoT device in an environment of its laboratory (US47, US52, US54).
     *
     * @return the located equipment, NOT_FOUND when the equipment or environment is not in the laboratory,
     * or CONFLICT when the environment already has an environmental device
     * @see AssignEquipmentToEnvironmentCommand
     */
    Result<Equipment, ApplicationError> handle(AssignEquipmentToEnvironmentCommand command);

    /**
     * Registers a change in the operational status of an equipment located in the environment (US48).
     *
     * @return the status change, NOT_FOUND when the equipment is not in the environment, or VALIDATION_ERROR
     * when the requested status is the current one
     * @see ChangeEquipmentStatusCommand
     */
    Result<EquipmentStatusChange, ApplicationError> handle(ChangeEquipmentStatusCommand command);

    /**
     * Handles the command to link an external IoT sensor to an existing equipment.
     *
     * @param command The command containing the equipment ID and the sensor's external ID.
     * @return A Result containing the ID of the updated equipment, or an ApplicationError.
     * @see LinkSensorCommand
     */
    Result<Long, ApplicationError> handle(LinkSensorCommand command);
}
