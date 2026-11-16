package com.iotech.qualitrack.platform.equipment.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request resource for registering an Edge Gateway.
 *
 * <p>Contains only the information required to identify
 * and register the Gateway in Equipment.</p>
 */
@Schema(name = "RegisterEdgeGatewayRequest", description = "Data required to register a laboratory Edge Gateway")
public record RegisterEdgeGatewayResource(

        @Schema(description = "Gateway display name", example = "Edge Gateway - Production")
        @NotBlank
        @Size(max = 150)
        String name,

        @Schema(description = "Unique external Gateway identifier", example = "EDGE-001")
        @NotBlank
        @Size(max = 50)
        String sensorExternalId,

        @Schema(description = "Unique equipment serial number", example = "RPI-SN-001")
        @NotBlank
        @Size(max = 50)
        String serialNumber,

        @Schema(description = "Gateway hardware model", example = "Raspberry Pi 4")
        @NotBlank
        @Size(max = 100)
        String model
) {}