package com.iotech.qualitrack.platform.profile.interfaces.acl;

/**
 * ACL facade exposed by the Profile bounded context.
 */
public interface ProfileContextFacade {

    /**
     * Name to show for a person in messages of other contexts, for example "acknowledged by María Pérez".
     *
     * @param userId the account
     * @return the full name of the profile, the username when the profile has no name, or null for an unknown account
     */
    String displayNameOf(Long userId);
}
