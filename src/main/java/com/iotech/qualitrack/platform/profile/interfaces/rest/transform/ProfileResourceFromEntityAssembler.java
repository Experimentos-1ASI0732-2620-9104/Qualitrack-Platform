package com.iotech.qualitrack.platform.profile.interfaces.rest.transform;

import com.iotech.qualitrack.platform.profile.domain.model.aggregates.Profile;
import com.iotech.qualitrack.platform.profile.domain.model.valueobjects.ProfileDetail;
import com.iotech.qualitrack.platform.profile.interfaces.rest.resources.ProfileResource;

/**
 * Maps profiles to REST resources.
 */
public final class ProfileResourceFromEntityAssembler {

    private ProfileResourceFromEntityAssembler() {
    }

    public static ProfileResource toResourceFromDetail(ProfileDetail detail) {
        Profile profile = detail.profile();
        return new ProfileResource(profile.getUserId(), detail.staffId(), detail.username(), detail.email(), detail.roles(),
                profile.getFullNameValue(),
                profile.getDni() == null ? null : profile.getDni().value(),
                profile.getPhoneNumber() == null ? null : profile.getPhoneNumber().value(),
                profile.getLocation() == null ? null : profile.getLocation().value(),
                detail.position(),
                profile.hasPhoto(),
                profile.hasPhoto() ? profile.getPhoto().updatedAt().toString() : null,
                profile.getUpdatedAt() == null ? null : profile.getUpdatedAt().toString());
    }
}
