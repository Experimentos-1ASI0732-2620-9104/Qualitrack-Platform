package com.iotech.qualitrack.platform.profile.domain.model.events;

/**
 * Domain event raised when a person changes the full name of the profile.
 *
 * @param userId the account of the profile
 * @param fullName new full name
 */
public record ProfileUpdatedEvent(Long userId, String fullName) {
}
