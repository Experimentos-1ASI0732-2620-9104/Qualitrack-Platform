package com.iotech.qualitrack.platform.tracking.domain.model.valueobjects;

import com.iotech.qualitrack.platform.tracking.domain.model.valueobjects.MonitoredMetric.DeviceKind;

/**
 * IoT device located in an environment whose readings, profile and actions Tracking &amp; Telemetry records.
 *
 * <p>Tracking's own view of an equipment of Equipment Management, translated by the anti-corruption layer so the
 * domain does not depend on that context.</p>
 *
 * @param id         device (equipment) identifier
 * @param deviceType ENVIRONMENTAL_DEVICE or CONTAINER_MONITOR
 */
public record MonitoredDevice(Long id, String deviceType) {

    /**
     * @return true when the device is a container monitor
     */
    public boolean isContainerMonitor() {
        return DeviceKind.CONTAINER_MONITOR.name().equals(deviceType);
    }
}
