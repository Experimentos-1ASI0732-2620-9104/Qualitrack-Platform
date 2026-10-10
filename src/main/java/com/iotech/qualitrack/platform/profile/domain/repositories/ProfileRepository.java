package com.iotech.qualitrack.platform.profile.domain.repositories;

import com.iotech.qualitrack.platform.profile.domain.model.aggregates.Profile;

import java.util.Optional;

/**
 * Profile repository port.
 */
public interface ProfileRepository {

    Optional<Profile> findByUserId(Long userId);

    Profile save(Profile profile);
}
