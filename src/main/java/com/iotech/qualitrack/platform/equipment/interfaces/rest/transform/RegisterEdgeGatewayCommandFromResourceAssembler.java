package com.iotech.qualitrack.platform.equipment.interfaces.rest.transform;

import com.iotech.qualitrack.platform.equipment.domain.model.commands.RegisterEdgeGatewayCommand;
import com.iotech.qualitrack.platform.equipment.interfaces.rest.resources.RegisterEdgeGatewayResource;

/**
 * Converts Edge Gateway REST requests into domain commands.
 */
public final class RegisterEdgeGatewayCommandFromResourceAssembler {

    /**
     * Creates a validated Edge Gateway registration command.
     *
     * @param laboratoryId owning laboratory identifier
     * @param resource Gateway registration request
     * @return Edge Gateway registration command
     */
    public static RegisterEdgeGatewayCommand toCommandFromResource(Long laboratoryId, RegisterEdgeGatewayResource resource) {

        return new RegisterEdgeGatewayCommand(
                laboratoryId,
                resource.name(),
                resource.sensorExternalId(),
                resource.serialNumber(),
                resource.model()
        );
    }
}