package com.iotech.qualitrack.platform.inventory.domain.model.valueobjects;

import java.time.Instant;
import java.util.Objects;

/**
 * Monitored container where a raw material lot is stored (US43): the container monitor, the environment of the
 * material and who stored the lot there.
 *
 * @param containerMonitorId the container monitor that represents the container
 * @param environmentId      the environment of the container and of the raw material
 * @param assignedBy         user who stored the lot in the container
 * @param assignedAt         moment of the assignment
 */
public record ContainerAssignment(Long containerMonitorId, Long environmentId, Long assignedBy, Instant assignedAt) {
    public ContainerAssignment {
        if (containerMonitorId == null || containerMonitorId <= 0) throw new IllegalArgumentException("Container is required");
        if (environmentId == null || environmentId <= 0) throw new IllegalArgumentException("Environment is required");
        Objects.requireNonNull(assignedAt, "Assignment time is required");
    }

    /**
     * @param otherContainerMonitorId a container monitor
     * @return true when the lot is already stored in that container
     */
    public boolean isIn(Long otherContainerMonitorId) {
        return containerMonitorId.equals(otherContainerMonitorId);
    }
}
