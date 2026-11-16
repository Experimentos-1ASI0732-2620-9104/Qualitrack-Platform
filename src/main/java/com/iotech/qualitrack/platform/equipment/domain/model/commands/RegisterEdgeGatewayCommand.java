package com.iotech.qualitrack.platform.equipment.domain.model.commands;

import static com.iotech.qualitrack.platform.equipment.domain.model.commands.CommandText.identifier;
import static com.iotech.qualitrack.platform.equipment.domain.model.commands.CommandText.required;

/**
 * Command to register an Edge Gateway in a laboratory.
 *
 * <p>Edge Gateways use their external identifier as a
 * public identity for subsequent cloud authentication.</p>
 *
 * @param laboratoryId laboratory that owns the Gateway
 * @param name Gateway display name
 * @param sensorExternalId unique external identifier
 * @param serialNumber unique equipment serial number
 * @param model Gateway hardware model
 */
public record RegisterEdgeGatewayCommand(
        Long laboratoryId,
        String name,
        String sensorExternalId,
        String serialNumber,
        String model
) {

    /**
     * Validates the required Gateway registration data.
     */
    public RegisterEdgeGatewayCommand {
        identifier(laboratoryId, "laboratoryId");
        name = required(name, "name", 150);
        sensorExternalId = required(
                sensorExternalId, "sensorExternalId", 50
        );
        serialNumber = required(
                serialNumber, "serialNumber", 50
        );
        model = required(model, "model", 100);
    }
}