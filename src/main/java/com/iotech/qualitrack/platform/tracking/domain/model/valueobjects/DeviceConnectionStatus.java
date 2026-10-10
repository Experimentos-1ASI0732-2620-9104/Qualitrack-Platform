package com.iotech.qualitrack.platform.tracking.domain.model.valueobjects;

/**
 * Whether an IoT device is communicating with the platform through Edge (US55).
 */
public enum DeviceConnectionStatus {
    /** The device communicated within the expected period. */
    CONNECTED,
    /** The device has not communicated within the expected period, or never did, and requires review. */
    REQUIRES_REVIEW
}
