package com.iotech.qualitrack.platform.batch.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Monitored container where a product batch is stored.
 *
 * @param containerMonitorId container monitor located in a product storage environment
 */
@Schema(name = "AssignBatchContainerRequest", description = "Container where the product batch is stored",
        example = "{\"containerMonitorId\": 12}")
public record AssignBatchContainerResource(
        @Schema(description = "Container monitor located in a product storage environment", example = "12")
        Long containerMonitorId
) {
}
