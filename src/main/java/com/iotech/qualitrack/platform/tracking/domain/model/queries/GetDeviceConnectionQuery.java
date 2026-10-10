package com.iotech.qualitrack.platform.tracking.domain.model.queries;

/**
 * Query for the connection state of an IoT device located in an environment (US55, TS41).
 *
 * @param laboratoryId laboratory that owns the device
 * @param environmentId environment where the device is located
 * @param deviceId device (equipment) identifier
 */
public record GetDeviceConnectionQuery(Long laboratoryId, Long environmentId, Long deviceId) {
    public GetDeviceConnectionQuery {
        if (laboratoryId == null || laboratoryId <= 0) throw new IllegalArgumentException("laboratoryId must be a positive number");
        if (environmentId == null || environmentId <= 0) throw new IllegalArgumentException("environmentId must be a positive number");
        if (deviceId == null || deviceId <= 0) throw new IllegalArgumentException("deviceId must be a positive number");
    }
}
