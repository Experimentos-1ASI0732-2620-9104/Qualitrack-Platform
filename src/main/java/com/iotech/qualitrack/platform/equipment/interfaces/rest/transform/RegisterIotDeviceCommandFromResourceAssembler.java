package com.iotech.qualitrack.platform.equipment.interfaces.rest.transform;

import com.iotech.qualitrack.platform.equipment.domain.model.commands.RegisterIotDeviceCommand;
import com.iotech.qualitrack.platform.equipment.domain.model.valueobjects.IotDeviceType;
import com.iotech.qualitrack.platform.equipment.interfaces.rest.resources.RegisterIotDeviceResource;

/**
 * Assembler to convert a RegisterIotDeviceResource and the device type of the path into a command.
 */
public class RegisterIotDeviceCommandFromResourceAssembler {

    public static RegisterIotDeviceCommand toCommandFromResource(Long laboratoryId, IotDeviceType deviceType,
                                                                 RegisterIotDeviceResource resource) {
        return new RegisterIotDeviceCommand(laboratoryId, deviceType, resource.name(), resource.sensorExternalId(),
                resource.serialNumber(), resource.model(), resource.firmwareVersion());
    }
}
