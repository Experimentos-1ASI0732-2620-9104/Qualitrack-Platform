package com.iotech.qualitrack.platform.profile.domain.model.commands;

/**
 * Command to remove the photo of the profile of the authenticated user.
 *
 * @param userId the account of the profile
 */
public record RemoveProfilePhotoCommand(Long userId) {
    public RemoveProfilePhotoCommand {
        if (userId == null || userId <= 0) throw new IllegalArgumentException("userId cannot be null or less than 1");
    }
}
