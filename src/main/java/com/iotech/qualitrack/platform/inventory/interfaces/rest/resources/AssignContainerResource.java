package com.iotech.qualitrack.platform.inventory.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Monitored container where a raw material lot is stored.
 *
 * @param containerMonitorId container monitor located in the environment of the raw material
 */
@Schema(name = "AssignRawMaterialBatchContainerRequest", description = "Container where the raw material lot is stored",
        example = "{\"containerMonitorId\": 12}")
public record AssignContainerResource(
        @Schema(description = "Container monitor located in the environment of the raw material", example = "12")
        Long containerMonitorId
) {
}
