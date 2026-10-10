package com.iotech.qualitrack.platform.equipment.domain.model.valueobjects;

/**
 * Kinds of ESP32 based IoT devices that QualiTrack recognises as equipment (US51, US53).
 *
 * <p>Equipment without a device type is ordinary laboratory equipment and produces no telemetry.
 * Measurements, thresholds and actuation of these devices belong to Tracking &amp; Telemetry.</p>
 */
public enum IotDeviceType {
    /** Node that supervises the conditions of a whole environment; an environment has at most one. */
    ENVIRONMENTAL_DEVICE,
    /** Monitor of a container inside an environment where raw material lots or product batches are kept. */
    CONTAINER_MONITOR
}
