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

    public static DeviceConnection evaluate(Long deviceId, Instant lastCommunicationAt, Instant now,
                                            ExpectedCommunicationPeriod expectedPeriod) {
        var connected = lastCommunicationAt != null
                && !lastCommunicationAt.isBefore(now.minus(expectedPeriod.value()));
        return new DeviceConnection(deviceId, lastCommunicationAt,
                connected ? DeviceConnectionStatus.CONNECTED : DeviceConnectionStatus.REQUIRES_REVIEW);
    }
}
