package com.iotech.qualitrack.platform.laboratory.domain.model.commands;

/**
 * Command to update the identification data of an existing environment.
 *
 * @param laboratoryId The laboratory that owns the environment. Cannot be null or less than 1.
 * @param environmentId The environment to update. Cannot be null or less than 1.
 * @param code The new identification, unique within the laboratory. Cannot be blank.
 * @param name The new display name. Cannot be blank.
 * @param description Optional description of the environment.
 */
public record UpdateEnvironmentCommand(
        Long laboratoryId,
        Long environmentId,
        String code,
        String name,
        String description
) {
    /**
     * Compact constructor for UpdateEnvironmentCommand.
     * Enforces Fail-Fast validation.
     */
    public UpdateEnvironmentCommand {
        if (laboratoryId == null || laboratoryId <= 0) {
            throw new IllegalArgumentException("laboratoryId cannot be null or less than 1");
        }
        if (environmentId == null || environmentId <= 0) {
            throw new IllegalArgumentException("environmentId cannot be null or less than 1");
        }
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("code cannot be null or blank");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name cannot be null or blank");
        }
    }
}
