package com.iotech.qualitrack.platform.equipment.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body to register an equipment in the laboratory of the path.
 */
@Schema(name = "RegisterEquipmentRequest", description = "Equipment to register in the laboratory")
public record RegisterEquipmentResource(
        @Schema(description = "Equipment display name", example = "Centrifuge 5000")
        @NotBlank @Size(max = 150) String name,
        @Schema(description = "Equipment category or type", example = "Centrifuge")
        @NotBlank @Size(max = 100) String type,
        @Schema(description = "Manufacturer model", example = "C-5000X")
        @NotBlank @Size(max = 100) String model,
        @Schema(description = "Unique serial number", example = "SN-987654321")
        @NotBlank @Size(max = 50) String serialNumber
) {
}
