package com.iotech.qualitrack.platform.ca.domain.model.commands;

import java.time.Instant;

/**
 * Command to note that the condition of an open alert returned to normal; the alert stays open until a person
 * resolves it (decision of 2026-10-04).
 *
 * @param laboratoryId  the laboratory of the device
 * @param deviceId      the environmental device or container monitor that measured the normal value
 * @param parameterName the monitored variable
 * @param normalizedAt  when the normal value was measured
 */
public record RecordConditionNormalizedCommand(Long laboratoryId, Long deviceId, String parameterName, Instant normalizedAt) {
    public RecordConditionNormalizedCommand {
        if (laboratoryId == null || laboratoryId <= 0) throw new IllegalArgumentException("laboratoryId cannot be null or less than 1");
        if (deviceId == null || deviceId <= 0) throw new IllegalArgumentException("deviceId cannot be null or less than 1");
        if (parameterName == null || parameterName.isBlank()) throw new IllegalArgumentException("parameterName cannot be null or blank");
        if (normalizedAt == null) throw new IllegalArgumentException("normalizedAt cannot be null");
        parameterName = parameterName.trim().toUpperCase();
    }
}
