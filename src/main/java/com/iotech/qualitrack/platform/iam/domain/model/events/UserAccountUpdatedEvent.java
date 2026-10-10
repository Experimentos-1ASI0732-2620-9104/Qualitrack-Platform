package com.iotech.qualitrack.platform.iam.domain.model.events;

/**
 * Domain event raised when a user changes the username or the e-mail of the account.
 *
 * @param userId the account
 * @param laboratoryId laboratory of the account, or null before the onboarding
 * @param username new username
 * @param email new e-mail address
 */
public record UserAccountUpdatedEvent(Long userId, Long laboratoryId, String username, String email) {
}
