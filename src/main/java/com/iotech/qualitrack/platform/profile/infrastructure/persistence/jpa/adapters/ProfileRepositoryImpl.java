package com.iotech.qualitrack.platform.profile.infrastructure.persistence.jpa.adapters;

import com.iotech.qualitrack.platform.profile.domain.model.aggregates.Profile;
import com.iotech.qualitrack.platform.profile.domain.repositories.ProfileRepository;
import com.iotech.qualitrack.platform.profile.infrastructure.persistence.jpa.assemblers.ProfilePersistenceAssembler;
import com.iotech.qualitrack.platform.profile.infrastructure.persistence.jpa.entities.ProfilePersistenceEntity;
import com.iotech.qualitrack.platform.profile.infrastructure.persistence.jpa.repositories.ProfilePersistenceRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class ProfileRepositoryImpl implements ProfileRepository {
    private final ProfilePersistenceRepository profiles;

    public ProfileRepositoryImpl(ProfilePersistenceRepository profiles) {
        this.profiles = profiles;
    }

    @Override
    public Optional<Profile> findByUserId(Long userId) {
        return profiles.findByUserId(userId).map(ProfilePersistenceAssembler::toDomainFromPersistence);
    }

    /**
     * Flushes the change so that the returned profile carries the date of this update.
     */
    @Override
    public Profile save(Profile profile) {
        var entity = profile.getId() == null ? new ProfilePersistenceEntity()
                : profiles.findById(profile.getId()).orElseGet(ProfilePersistenceEntity::new);
        var saved = profiles.saveAndFlush(ProfilePersistenceAssembler.toPersistenceFromDomain(profile, entity));
        return ProfilePersistenceAssembler.toDomainFromPersistence(saved);
    }
}
