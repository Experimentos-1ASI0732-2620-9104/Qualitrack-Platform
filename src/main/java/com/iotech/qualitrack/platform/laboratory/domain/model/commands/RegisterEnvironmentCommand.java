package com.iotech.qualitrack.platform.laboratory.domain.model.commands;

/**
 * Command to register a new environment inside a laboratory.
 *
 * @param laboratoryId The laboratory that owns the environment. Cannot be null or less than 1.
 * @param code The identification of the environment, unique within the laboratory. Cannot be blank.
 * @param name The display name of the environment. Cannot be blank.
 * @param description Optional description of the environment.
 */
public record RegisterEnvironmentCommand(
        Long laboratoryId,
        String code,
        String name,
        String description
) {
    /**
     * Compact constructor for RegisterEnvironmentCommand.
     * Enforces Fail-Fast validation.
     */
    public RegisterEnvironmentCommand {
        if (laboratoryId == null || laboratoryId <= 0) {
            throw new IllegalArgumentException("laboratoryId cannot be null or less than 1");
        }
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("code cannot be null or blank");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name cannot be null or blank");
        }
    }
}
