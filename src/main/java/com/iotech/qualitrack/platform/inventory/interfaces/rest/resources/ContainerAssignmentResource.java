package com.iotech.qualitrack.platform.inventory.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Monitored container where a raw material lot is stored and its environment (US44).
 *
 * @param rawMaterialBatchId the raw material lot
 * @param containerMonitorId the container monitor that represents the container
 * @param containerName      the container monitor name, or null when it is no longer registered
 * @param environmentId      the environment of the container and of the raw material
 * @param assignedBy         user who stored the lot in the container
 * @param assignedAt         moment of the assignment
 */
@Schema(name = "RawMaterialBatchContainerResponse", description = "Container and environment where the raw material lot is stored")
public record ContainerAssignmentResource(Long rawMaterialBatchId, Long containerMonitorId, String containerName,
                                          Long environmentId, Long assignedBy, Instant assignedAt) {
}
