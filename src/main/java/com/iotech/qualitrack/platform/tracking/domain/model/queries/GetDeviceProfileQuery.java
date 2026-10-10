package com.iotech.qualitrack.platform.tracking.domain.model.queries;

/**
 * Gets the profile a device applies (TS57): the profile of the environment for its environmental device, or the
 * profile of the container monitor.
 */
public record GetDeviceProfileQuery(Long laboratoryId, Long environmentId, Long deviceId) {
}
