package com.iotech.qualitrack.platform.tracking.domain.model.valueobjects;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Quantity reported by an IoT device of a laboratory.
 *
 * <p>The environmental device of an environment reports the air quality and the motion detected in the zone; a
 * container monitor reports the temperature, humidity and luminosity inside the container and the RFID tag it reads.
 * Each metric has a fixed unit so that the configured thresholds and the measurements are always comparable.</p>
 */
public enum MonitoredMetric {
    /** Air quality of the environment in parts per million; a higher value means worse air. */
    AIR_QUALITY("ppm", DeviceKind.ENVIRONMENTAL_DEVICE, Reading.NUMERIC),
    /** Motion detected by the environmental device: 1 when detected, 0 when the zone is quiet again. */
    MOTION("event", DeviceKind.ENVIRONMENTAL_DEVICE, Reading.DETECTION),
    /** Temperature inside the container in degrees Celsius. */
    TEMPERATURE("°C", DeviceKind.CONTAINER_MONITOR, Reading.NUMERIC),
    /** Relative humidity in percent. */
    HUMIDITY("%RH", DeviceKind.CONTAINER_MONITOR, Reading.NUMERIC),
    /** Light inside the container in lux. */
    LUMINOSITY("lux", DeviceKind.CONTAINER_MONITOR, Reading.NUMERIC),
    /** Identifier of the RFID tag read on the container. */
    RFID_TAG("tag", DeviceKind.CONTAINER_MONITOR, Reading.TEXT);

    private final String unit;
    private final DeviceKind deviceKind;
    private final Reading reading;

    MonitoredMetric(String unit, DeviceKind deviceKind, Reading reading) {
        this.unit = unit;
        this.deviceKind = deviceKind;
        this.reading = reading;
    }

    /**
     * @return the fixed unit of the metric, used by its thresholds and readings
     */
    public String unit() {
        return unit;
    }

    /**
     * Whether the quality manager can configure WARNING/CRITICAL thresholds for the metric; only numeric metrics
     * accept them, so MOTION and RFID_TAG are recorded but never evaluated.
     */
    public boolean hasThresholds() {
        return reading == Reading.NUMERIC;
    }

    /**
     * Whether the reading carries a text value (the RFID tag) instead of a number.
     */
    public boolean isText() {
        return reading == Reading.TEXT;
    }

    /**
     * Whether the reading is a detection whose value is 1 when detected and 0 otherwise.
     */
    public boolean isDetection() {
        return reading == Reading.DETECTION;
    }

    /**
     * Whether the device type (as named by Equipment Management) reports this metric.
     */
    public boolean isReportedBy(String deviceType) {
        return deviceKind.name().equals(deviceType);
    }

    /**
     * @param deviceType device type as named by Equipment Management
     * @return the metrics reported by that type of device, in declaration order
     */
    public static List<MonitoredMetric> reportedBy(String deviceType) {
        return Arrays.stream(values()).filter(metric -> metric.isReportedBy(deviceType)).toList();
    }

    /**
     * Reads a metric name, ignoring case and surrounding spaces.
     *
     * @return the metric; empty when the value is blank or unknown
     */
    public static Optional<MonitoredMetric> parse(String value) {
        if (value == null || value.isBlank()) return Optional.empty();
        try {
            return Optional.of(valueOf(value.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    /**
     * IoT device types of Equipment Management, mirrored to keep Tracking independent from that context.
     */
    public enum DeviceKind {
        ENVIRONMENTAL_DEVICE,
        CONTAINER_MONITOR
    }

    /**
     * Kind of value a reading of the metric carries.
     */
    private enum Reading {
        NUMERIC,
        DETECTION,
        TEXT
    }
}
