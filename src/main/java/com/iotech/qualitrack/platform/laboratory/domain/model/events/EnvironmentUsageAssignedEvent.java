package com.iotech.qualitrack.platform.laboratory.domain.model.events;

import com.iotech.qualitrack.platform.laboratory.domain.model.aggregates.Environment;
import com.iotech.qualitrack.platform.laboratory.domain.model.valueobjects.EnvironmentUsage;

import java.time.Instant;

/**
 * Domain event published when the main use of an environment is assigned.
 *
 * @param environmentId The numeric identity of the environment.
 * @param laboratoryId The laboratory that owns the environment.
 * @param usage The assigned usage.
 * @param assignedBy The user that assigned the usage.
 * @param assignedAt The moment of the assignment.
 */
public record EnvironmentUsageAssignedEvent(
        Long environmentId,
        Long laboratoryId,
        EnvironmentUsage usage,
        Long assignedBy,
        Instant assignedAt) {

    public static EnvironmentUsageAssignedEvent from(Environment environment) {
        return new EnvironmentUsageAssignedEvent(
                environment.getId(),
                environment.getLaboratoryId(),
                environment.getUsage(),
                environment.getUsageAssignedBy(),
                environment.getUsageAssignedAt());
    }
}
