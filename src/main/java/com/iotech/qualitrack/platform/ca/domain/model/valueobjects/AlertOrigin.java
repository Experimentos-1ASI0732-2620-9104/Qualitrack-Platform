package com.iotech.qualitrack.platform.ca.domain.model.valueobjects;

/**
 * Where a deviation alert originated (US86): the environment, supervised by its environmental device, or a monitored
 * container of the environment, supervised by its container monitor.
 */
public enum AlertOrigin {
    ENVIRONMENT,
    CONTAINER;

    /**
     * @param deviceType IoT device type of the device that detected the deviation
     * @return the origin of the alert
     * @throws IllegalArgumentException when the device type is not an environmental device nor a container monitor
     */
    public static AlertOrigin fromDeviceType(String deviceType) {
        return switch (deviceType == null ? "" : deviceType) {
            case "ENVIRONMENTAL_DEVICE" -> ENVIRONMENT;
            case "CONTAINER_MONITOR" -> CONTAINER;
            default -> throw new IllegalArgumentException("Unknown IoT device type: " + deviceType);
        };
    }
}
