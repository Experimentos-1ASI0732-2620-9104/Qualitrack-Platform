package com.iotech.qualitrack.platform.equipment.domain.model.commands;

import com.iotech.qualitrack.platform.equipment.domain.model.valueobjects.IotDeviceType;

import static com.iotech.qualitrack.platform.equipment.domain.model.commands.CommandText.identifier;
import static com.iotech.qualitrack.platform.equipment.domain.model.commands.CommandText.optional;
import static com.iotech.qualitrack.platform.equipment.domain.model.commands.CommandText.required;

/**
 * Command to register an ESP32 environmental device or container monitor (US51, US53, TS37, TS39).
 *
 * @param laboratoryId laboratory that owns the device
 * @param deviceType environmental device or container monitor
 * @param name display name, up to 150 characters
 * @param sensorExternalId identifier with which Edge recognises the device, unique, up to 50 characters
 * @param serialNumber MAC address or serial number, unique, up to 50 characters
 * @param model hardware model, up to 100 characters
 * @param firmwareVersion optional firmware version, up to 50 characters
 */
public record RegisterIotDeviceCommand(
        Long laboratoryId,
        IotDeviceType deviceType,
        String name,
        String sensorExternalId,
        String serialNumber,
        String model,
        String firmwareVersion
) {
    public RegisterIotDeviceCommand {
        identifier(laboratoryId, "laboratoryId");
        if (deviceType == null) throw new IllegalArgumentException("deviceType is required");
        name = required(name, "name", 150);
        sensorExternalId = required(sensorExternalId, "sensorExternalId", 50);
        serialNumber = required(serialNumber, "serialNumber", 50);
        model = required(model, "model", 100);
        firmwareVersion = optional(firmwareVersion, "firmwareVersion", 50);
    }
}
