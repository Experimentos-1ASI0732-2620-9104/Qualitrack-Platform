package com.iotech.qualitrack.platform.profile.application.acl;

import com.iotech.qualitrack.platform.profile.application.queryservices.ProfileQueryService;
import com.iotech.qualitrack.platform.profile.domain.model.queries.GetProfileByUserIdQuery;
import com.iotech.qualitrack.platform.profile.interfaces.acl.ProfileContextFacade;
import org.springframework.stereotype.Service;

/**
 * Application-layer implementation of the {@link ProfileContextFacade} ACL facade.
 */
@Service
public class ProfileContextFacadeImpl implements ProfileContextFacade {

    private final ProfileQueryService profileQueryService;

    public ProfileContextFacadeImpl(ProfileQueryService profileQueryService) {
        this.profileQueryService = profileQueryService;
    }

    @Override
    public String displayNameOf(Long userId) {
        if (userId == null || userId <= 0) return null;
        return profileQueryService.handle(new GetProfileByUserIdQuery(userId))
                .map(detail -> detail.profile().getFullNameValue() != null ? detail.profile().getFullNameValue() : detail.username())
                .orElse(null);
    }
}
