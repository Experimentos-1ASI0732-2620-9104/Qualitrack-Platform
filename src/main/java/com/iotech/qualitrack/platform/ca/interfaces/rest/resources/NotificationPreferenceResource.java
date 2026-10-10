package com.iotech.qualitrack.platform.ca.interfaces.rest.resources;

import com.iotech.qualitrack.platform.ca.domain.model.valueobjects.AlertSeverity;

public record NotificationPreferenceResource(
        Long id,
        Long userId,
        Boolean emailEnabled,
        Boolean inAppEnabled,
        AlertSeverity minimumSeverity
) {
}