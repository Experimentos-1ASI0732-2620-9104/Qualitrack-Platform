package com.iotech.qualitrack.platform.profile.domain.model.commands;

import com.iotech.qualitrack.platform.profile.domain.model.valueobjects.PhotoImage;

/**
 * Command to replace the photo of the profile of the authenticated user.
 *
 * @param userId the account of the profile
 * @param image the new photo
 */
public record ChangeProfilePhotoCommand(Long userId, PhotoImage image) {
    public ChangeProfilePhotoCommand {
        if (userId == null || userId <= 0) throw new IllegalArgumentException("userId cannot be null or less than 1");
        if (image == null) throw new IllegalArgumentException("The photo is required");
    }
}
