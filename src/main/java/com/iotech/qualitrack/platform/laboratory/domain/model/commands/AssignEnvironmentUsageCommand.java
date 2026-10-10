package com.iotech.qualitrack.platform.laboratory.domain.model.commands;

import com.iotech.qualitrack.platform.laboratory.domain.model.valueobjects.EnvironmentUsage;

/**
 * Command to assign the main use of an environment.
 *
 * @param laboratoryId The laboratory that owns the environment. Cannot be null or less than 1.
 * @param environmentId The environment that receives the usage. Cannot be null or less than 1.
 * @param usage The allowed usage to assign. Cannot be null.
 */
public record AssignEnvironmentUsageCommand(
        Long laboratoryId,
        Long environmentId,
        EnvironmentUsage usage
) {
    /**
     * Compact constructor for AssignEnvironmentUsageCommand.
     * Enforces Fail-Fast validation.
     */
    public AssignEnvironmentUsageCommand {
        if (laboratoryId == null || laboratoryId <= 0) {
            throw new IllegalArgumentException("laboratoryId cannot be null or less than 1");
        }
        if (environmentId == null || environmentId <= 0) {
            throw new IllegalArgumentException("environmentId cannot be null or less than 1");
        }
        if (usage == null) {
            throw new IllegalArgumentException("usage cannot be null");
        }
    }
}
