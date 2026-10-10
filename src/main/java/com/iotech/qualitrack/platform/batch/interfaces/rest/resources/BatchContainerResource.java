package com.iotech.qualitrack.platform.batch.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Monitored container where a product batch is stored and its environment (US79).
 */
@Schema(name = "BatchContainerResponse", description = "Container and environment where the product batch is stored")
public record BatchContainerResource(
        @Schema(description = "Product batch", example = "8") Long batchId,
        @Schema(description = "Container monitor that represents the container", example = "12") Long containerMonitorId,
        @Schema(description = "Container monitor name; null when it is no longer registered", example = "Cold room shelf B", nullable = true)
        String containerName,
        @Schema(description = "Product storage environment of the container", example = "4") Long environmentId,
        @Schema(description = "User who stored the batch in the container", example = "7") Long assignedBy,
        @Schema(description = "Moment of the assignment", example = "2026-10-04T14:05:00Z") Instant assignedAt
) {
}
