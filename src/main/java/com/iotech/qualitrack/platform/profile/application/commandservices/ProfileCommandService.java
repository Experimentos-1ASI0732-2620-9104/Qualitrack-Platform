package com.iotech.qualitrack.platform.profile.application.commandservices;

import com.iotech.qualitrack.platform.profile.domain.model.aggregates.Profile;
import com.iotech.qualitrack.platform.profile.domain.model.commands.ChangeProfilePhotoCommand;
import com.iotech.qualitrack.platform.profile.domain.model.commands.RemoveProfilePhotoCommand;
import com.iotech.qualitrack.platform.profile.domain.model.commands.UpdateProfileCommand;
import com.iotech.qualitrack.platform.shared.application.result.ApplicationError;
import com.iotech.qualitrack.platform.shared.application.result.Result;

/**
 * Changes the profile of the authenticated user. The profile is created the first time it is saved.
 */
public interface ProfileCommandService {

    Result<Profile, ApplicationError> handle(UpdateProfileCommand command);

    Result<Profile, ApplicationError> handle(ChangeProfilePhotoCommand command);

    /**
     * Removing a photo that does not exist changes nothing.
     */
    Result<Profile, ApplicationError> handle(RemoveProfilePhotoCommand command);
}
