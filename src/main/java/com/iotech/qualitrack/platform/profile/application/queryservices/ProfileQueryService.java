package com.iotech.qualitrack.platform.profile.application.queryservices;

import com.iotech.qualitrack.platform.profile.domain.model.queries.GetProfileByUserIdQuery;
import com.iotech.qualitrack.platform.profile.domain.model.queries.GetProfilePhotoQuery;
import com.iotech.qualitrack.platform.profile.domain.model.queries.GetStaffProfilePhotoQuery;
import com.iotech.qualitrack.platform.profile.domain.model.queries.GetStaffProfileQuery;
import com.iotech.qualitrack.platform.profile.domain.model.valueobjects.PhotoImage;
import com.iotech.qualitrack.platform.profile.domain.model.valueobjects.ProfileDetail;

import java.util.Optional;

/**
 * Reads profiles and their photos.
 */
public interface ProfileQueryService {

    /**
     * @return the profile with its account; empty when the account does not exist. A profile never saved is returned
     * unsaved, with the name the quality manager registered for a staff member.
     */
    Optional<ProfileDetail> handle(GetProfileByUserIdQuery query);

    Optional<PhotoImage> handle(GetProfilePhotoQuery query);

    /**
     * @return the profile of the staff member; empty when the staff member does not exist or has no account
     */
    Optional<ProfileDetail> handle(GetStaffProfileQuery query);

    Optional<PhotoImage> handle(GetStaffProfilePhotoQuery query);
}
