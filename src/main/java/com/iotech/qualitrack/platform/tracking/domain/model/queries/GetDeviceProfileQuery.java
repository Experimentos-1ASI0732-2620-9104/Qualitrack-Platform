package com.iotech.qualitrack.platform.tracking.domain.model.queries;

/**
 * Gets the profile a device applies (TS57): the profile of the environment for its environmental device, or the
 * profile of the container monitor.
 *
 * @param laboratoryId  laboratory that owns the device
 * @param environmentId environment where the device is located
 * @param deviceId      environmental device or container monitor (equipment)
 */
public record GetDeviceProfileQuery(Long laboratoryId, Long environmentId, Long deviceId) {
}
