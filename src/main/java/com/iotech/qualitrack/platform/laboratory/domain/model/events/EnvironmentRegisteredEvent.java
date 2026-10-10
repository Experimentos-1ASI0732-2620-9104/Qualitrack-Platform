package com.iotech.qualitrack.platform.laboratory.domain.model.events;

import com.iotech.qualitrack.platform.laboratory.domain.model.aggregates.Environment;

/**
 * Domain event published when a new environment is registered in a laboratory.
 *
 * @param environmentId The numeric identity of the registered environment.
 * @param laboratoryId The laboratory that owns the environment.
 * @param code The identification of the environment.
 * @param name The display name of the environment.
 */
public record EnvironmentRegisteredEvent(
        Long environmentId,
        Long laboratoryId,
        String code,
        String name) {

    public static EnvironmentRegisteredEvent from(Environment environment) {
        return new EnvironmentRegisteredEvent(
                environment.getId(),
                environment.getLaboratoryId(),
                environment.getCode(),
                environment.getName());
    }
}
