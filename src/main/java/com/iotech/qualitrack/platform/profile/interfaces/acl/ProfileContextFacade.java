package com.iotech.qualitrack.platform.profile.interfaces.acl;

import com.iotech.qualitrack.platform.profile.application.queryservices.ProfileQueryService;
import com.iotech.qualitrack.platform.profile.domain.model.queries.GetProfileByUserIdQuery;
import org.springframework.stereotype.Service;

/**
 * ACL facade exposed by the Profile bounded context.
 */
@Service
public class ProfileContextFacade {

    private final ProfileQueryService profileQueryService;

    public ProfileContextFacade(ProfileQueryService profileQueryService) {
        this.profileQueryService = profileQueryService;
    }

    /**
     * Name to show for a person in messages of other contexts, for example "acknowledged by María Pérez".
     *
     * @param userId the account
     * @return the full name of the profile, the username when the profile has no name, or null for an unknown account
     */
    public String displayNameOf(Long userId) {
        if (userId == null || userId <= 0) return null;
        return profileQueryService.handle(new GetProfileByUserIdQuery(userId))
                .map(detail -> detail.profile().getFullNameValue() != null ? detail.profile().getFullNameValue() : detail.username())
                .orElse(null);
    }
}
