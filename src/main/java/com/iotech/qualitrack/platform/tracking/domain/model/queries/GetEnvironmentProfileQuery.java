package com.iotech.qualitrack.platform.tracking.domain.model.queries;

/**
 * Gets the environmental profile of an environment.
 */
public record GetEnvironmentProfileQuery(Long laboratoryId, Long environmentId) {
}
