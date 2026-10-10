package com.iotech.qualitrack.platform.profile.infrastructure.storage.database;

import com.iotech.qualitrack.platform.profile.application.internal.outboundservices.storage.ProfilePhotoStorage;
import com.iotech.qualitrack.platform.profile.domain.model.valueobjects.PhotoImage;
import com.iotech.qualitrack.platform.profile.infrastructure.persistence.jpa.entities.ProfilePhotoPersistenceEntity;
import com.iotech.qualitrack.platform.profile.infrastructure.persistence.jpa.repositories.ProfilePhotoPersistenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Keeps the profile photos in the database, one row per profile.
 */
@Service
public class DatabaseProfilePhotoStorage implements ProfilePhotoStorage {

    private final ProfilePhotoPersistenceRepository photos;

    public DatabaseProfilePhotoStorage(ProfilePhotoPersistenceRepository photos) {
        this.photos = photos;
    }

    @Override
    @Transactional
    public void store(Long profileId, PhotoImage image) {
        var entity = photos.findByProfileId(profileId).orElseGet(ProfilePhotoPersistenceEntity::new);
        entity.setProfileId(profileId);
        entity.setContentType(image.contentType());
        entity.setContent(image.content());
        photos.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PhotoImage> find(Long profileId) {
        return photos.findByProfileId(profileId)
                .map(entity -> new PhotoImage(entity.getContent(), entity.getContentType()));
    }

    @Override
    @Transactional
    public void remove(Long profileId) {
        photos.deleteByProfileId(profileId);
    }
}
