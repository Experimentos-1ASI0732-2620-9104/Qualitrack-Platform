package com.iotech.qualitrack.platform.profile.infrastructure.persistence.jpa.assemblers;

import com.iotech.qualitrack.platform.profile.domain.model.aggregates.Profile;
import com.iotech.qualitrack.platform.profile.domain.model.valueobjects.Dni;
import com.iotech.qualitrack.platform.profile.domain.model.valueobjects.Location;
import com.iotech.qualitrack.platform.profile.domain.model.valueobjects.PersonName;
import com.iotech.qualitrack.platform.profile.domain.model.valueobjects.PhoneNumber;
import com.iotech.qualitrack.platform.profile.domain.model.valueobjects.ProfilePhoto;
import com.iotech.qualitrack.platform.profile.infrastructure.persistence.jpa.entities.ProfilePersistenceEntity;

/**
 * Maps profiles between the domain and the persistence entity.
 */
public final class ProfilePersistenceAssembler {

    private ProfilePersistenceAssembler() {
    }

    public static Profile toDomainFromPersistence(ProfilePersistenceEntity entity) {
        var photo = entity.getPhotoContentType() == null ? null
                : new ProfilePhoto(entity.getPhotoContentType(), entity.getPhotoSizeBytes(), entity.getPhotoUpdatedAt());
        return new Profile(entity.getId(), entity.getUserId(),
                entity.getFullName() == null ? null : new PersonName(entity.getFullName()),
                entity.getDni() == null ? null : new Dni(entity.getDni()),
                entity.getPhoneNumber() == null ? null : new PhoneNumber(entity.getPhoneNumber()),
                entity.getLocation() == null ? null : new Location(entity.getLocation()),
                photo,
                entity.getUpdatedAt() == null ? null : entity.getUpdatedAt().toInstant());
    }

    public static ProfilePersistenceEntity toPersistenceFromDomain(Profile profile, ProfilePersistenceEntity entity) {
        entity.setId(profile.getId());
        entity.setUserId(profile.getUserId());
        entity.setFullName(profile.getFullNameValue());
        entity.setDni(profile.getDni() == null ? null : profile.getDni().value());
        entity.setPhoneNumber(profile.getPhoneNumber() == null ? null : profile.getPhoneNumber().value());
        entity.setLocation(profile.getLocation() == null ? null : profile.getLocation().value());
        var photo = profile.getPhoto();
        entity.setPhotoContentType(photo == null ? null : photo.contentType());
        entity.setPhotoSizeBytes(photo == null ? null : photo.sizeBytes());
        entity.setPhotoUpdatedAt(photo == null ? null : photo.updatedAt());
        return entity;
    }
}
