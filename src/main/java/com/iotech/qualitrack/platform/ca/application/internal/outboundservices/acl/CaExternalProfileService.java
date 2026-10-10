package com.iotech.qualitrack.platform.ca.application.internal.outboundservices.acl;

import com.iotech.qualitrack.platform.profile.interfaces.acl.ProfileContextFacade;
import org.springframework.stereotype.Service;

/**
 * Reads the names people use in QualiTrack, to say who attended an alert or decided on a batch.
 */
@Service
public class CaExternalProfileService {

    private final ProfileContextFacade profileContextFacade;

    public CaExternalProfileService(ProfileContextFacade profileContextFacade) {
        this.profileContextFacade = profileContextFacade;
    }

    /**
     * @return the full name of the person, the username when the profile has no name, or null
     */
    public String displayNameOf(Long userId) {
        return profileContextFacade.displayNameOf(userId);
    }
}
