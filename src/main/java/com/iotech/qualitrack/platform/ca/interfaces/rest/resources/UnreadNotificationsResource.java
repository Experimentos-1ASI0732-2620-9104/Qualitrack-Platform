package com.iotech.qualitrack.platform.ca.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "UnreadNotifications", description = "How many notifications the person has not read")
public record UnreadNotificationsResource(@Schema(example = "3") long unreadCount) {
}
