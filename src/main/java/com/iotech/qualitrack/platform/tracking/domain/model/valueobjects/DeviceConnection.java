package com.iotech.qualitrack.platform.tracking.domain.model.valueobjects;

import java.time.Instant;

/**
 * Connection state of an IoT device derived from its latest communication (US55, TS41).
 *
 * @param deviceId device (equipment) identifier
 * @param lastCommunicationAt moment of the latest telemetry or heartbeat received, or null if none
 * @param status connected, or requires review when the device is silent for longer than the expected period
 */
public record DeviceConnection(Long deviceId, Instant lastCommunicationAt, DeviceConnectionStatus status) {

    /**
     * Derives the connection state at a moment: CONNECTED when the device communicated within the expected period
     * (inclusive), REQUIRES_REVIEW when it is silent for longer or never communicated.
     *
     * @param deviceId            device (equipment) identifier
     * @param lastCommunicationAt latest reading or action received from the device, or null if none
     * @param now                 moment of the evaluation
     * @param expectedPeriod      maximum time between two communications
     * @return the connection state of the device
     */
    public static DeviceConnection evaluate(Long deviceId, Instant lastCommunicationAt, Instant now,
                                            ExpectedCommunicationPeriod expectedPeriod) {
        var connected = lastCommunicationAt != null
                && !lastCommunicationAt.isBefore(now.minus(expectedPeriod.value()));
        return new DeviceConnection(deviceId, lastCommunicationAt,
                connected ? DeviceConnectionStatus.CONNECTED : DeviceConnectionStatus.REQUIRES_REVIEW);
    }
}
