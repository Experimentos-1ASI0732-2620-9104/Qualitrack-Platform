package com.iotech.qualitrack.platform.tracking.domain.model.queries;

/**
 * Gets the environmental profile of a container monitor located in an environment.
 *
 * @param laboratoryId  laboratory that owns the device
 * @param environmentId environment where the container monitor is located
 * @param deviceId      container monitor (equipment)
 */
public record GetContainerMonitorProfileQuery(Long laboratoryId, Long environmentId, Long deviceId) {
}
