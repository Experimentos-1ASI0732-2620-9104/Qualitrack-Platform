package com.iotech.qualitrack.platform.batch.domain.model.valueobjects;

import java.time.Instant;
import java.util.Objects;

/**
 * Monitored container where a product batch is stored (US78): the container monitor, the product storage environment
 * where it is located and who stored the batch there.
 *
 * @param containerMonitorId the container monitor that represents the container
 * @param environmentId      the environment of the container
 * @param assignedBy         user who stored the batch in the container
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
     * @return true when the batch is already stored in that container
     */
    public boolean isIn(Long otherContainerMonitorId) {
        return containerMonitorId.equals(otherContainerMonitorId);
    }
}
