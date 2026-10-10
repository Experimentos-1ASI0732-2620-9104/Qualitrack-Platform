package com.iotech.qualitrack.platform.ca.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "NotificationsRead", description = "Result of marking every notification as read")
public record NotificationsReadResource(@Schema(description = "Notifications that were unread", example = "3") int markedAsRead) {
}
