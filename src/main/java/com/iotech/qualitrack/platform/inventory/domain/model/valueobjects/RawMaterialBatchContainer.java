package com.iotech.qualitrack.platform.inventory.domain.model.valueobjects;

import java.time.Instant;

/**
 * Read model of the monitored container where a raw material lot is stored (US44): the container and its environment.
 *
 * @param receiptId          the raw material lot
 * @param containerMonitorId the container monitor that represents the container
 * @param containerName      the name of the container monitor, or null when it is no longer registered
 * @param environmentId      the environment of the container and of the raw material
 * @param assignedBy         user who stored the lot in the container
 * @param assignedAt         moment of the assignment
 */
public record RawMaterialBatchContainer(Long receiptId, Long containerMonitorId, String containerName, Long environmentId,
                                        Long assignedBy, Instant assignedAt) { }
