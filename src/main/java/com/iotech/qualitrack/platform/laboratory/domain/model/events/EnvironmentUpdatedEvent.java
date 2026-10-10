package com.iotech.qualitrack.platform.laboratory.domain.model.events;

import com.iotech.qualitrack.platform.laboratory.domain.model.aggregates.Environment;

/**
 * Domain event published when the identification data of an environment changes.
 *
 * @param environmentId The numeric identity of the updated environment.
 * @param laboratoryId The laboratory that owns the environment.
 * @param code The current identification of the environment.
 * @param name The current display name of the environment.
 */
public record EnvironmentUpdatedEvent(
        Long environmentId,
        Long laboratoryId,
        String code,
        String name) {

    public static EnvironmentUpdatedEvent from(Environment environment) {
        return new EnvironmentUpdatedEvent(
                environment.getId(),
                environment.getLaboratoryId(),
                environment.getCode(),
                environment.getName());
    }
}
