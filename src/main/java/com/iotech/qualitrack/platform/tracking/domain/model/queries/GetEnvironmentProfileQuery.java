package com.iotech.qualitrack.platform.tracking.domain.model.queries;

/**
 * Gets the environmental profile of an environment.
 *
 * @param laboratoryId  laboratory that owns the environment
 * @param environmentId environment
 */
public record GetEnvironmentProfileQuery(Long laboratoryId, Long environmentId) {
}
