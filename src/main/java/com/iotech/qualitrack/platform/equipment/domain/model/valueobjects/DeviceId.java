package com.iotech.qualitrack.platform.equipment.domain.model.valueobjects;

/**
 * External identity of an IoT device ({@code sensorExternalId}), the id the Edge and the ESP32 firmware use for
 * the node, for example {@code ENV-001} or {@code CNT-001}.
 *
 * @param value device identity; required and up to 50 characters
 */
public record DeviceId(String value) {
    public DeviceId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Device ID cannot be null or blank");
        }
        if (value.length() > 50) {
            throw new IllegalArgumentException("Device ID cannot exceed 50 characters");
        }
    }
}