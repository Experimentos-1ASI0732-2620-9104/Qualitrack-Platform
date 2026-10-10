package com.iotech.qualitrack.platform.tracking.domain.model.queries;

/**
 * Gets the environmental profile of a container monitor located in an environment.
 */
public record GetContainerMonitorProfileQuery(Long laboratoryId, Long environmentId, Long deviceId) {
}
