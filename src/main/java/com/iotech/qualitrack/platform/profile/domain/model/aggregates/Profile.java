package com.iotech.qualitrack.platform.profile.domain.model.aggregates;

import com.iotech.qualitrack.platform.profile.domain.model.valueobjects.Dni;
import com.iotech.qualitrack.platform.profile.domain.model.valueobjects.Location;
import com.iotech.qualitrack.platform.profile.domain.model.valueobjects.PersonName;
import com.iotech.qualitrack.platform.profile.domain.model.valueobjects.PhoneNumber;
import com.iotech.qualitrack.platform.profile.domain.model.valueobjects.PhotoImage;
import com.iotech.qualitrack.platform.profile.domain.model.valueobjects.ProfilePhoto;
import com.iotech.qualitrack.platform.shared.domain.model.aggregates.AbstractDomainAggregateRoot;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.Objects;

/**
 * Profile aggregate root: the personal data a person keeps about themselves in QualiTrack, besides the sign-in
 * account kept by IAM. There is one profile per account; it is created the first time the person saves it.
 */
@Getter
public class Profile extends AbstractDomainAggregateRoot<Profile> {

    @Setter
    private Long id;

    /**
     * IAM account the profile belongs to.
     */
    private Long userId;

    /**
     * Full name; it may be missing until the person completes the profile.
     */
    private PersonName fullName;

    private Dni dni;

    private PhoneNumber phoneNumber;

    private Location location;

    /**
     * Description of the current photo, or null without photo.
     */
    private ProfilePhoto photo;

    /**
     * Last change of the profile, set by the persistence layer.
     */
    private Instant updatedAt;

    /**
     * Starts the profile of an account.
     *
     * @param userId the account
     * @param fullName name already known for the person, for example the one the quality manager registered, or null
     */
    public Profile(Long userId, PersonName fullName) {
        if (userId == null || userId <= 0) throw new IllegalArgumentException("userId cannot be null or less than 1");
        this.userId = userId;
        this.fullName = fullName;
    }

    /**
     * Reconstructs a stored profile.
     */
    public Profile(Long id, Long userId, PersonName fullName, Dni dni, PhoneNumber phoneNumber, Location location,
                   ProfilePhoto photo, Instant updatedAt) {
        this(userId, fullName);
        this.id = id;
        this.dni = dni;
        this.phoneNumber = phoneNumber;
        this.location = location;
        this.photo = photo;
        this.updatedAt = updatedAt;
    }

    /**
     * Replaces the personal data. The DNI, the phone number and the location are optional: null clears them.
     *
     * @return true when the full name changed
     */
    public boolean update(PersonName fullName, Dni dni, PhoneNumber phoneNumber, Location location) {
        Objects.requireNonNull(fullName, "The full name is required");
        var renamed = !fullName.equals(this.fullName);
        this.fullName = fullName;
        this.dni = dni;
        this.phoneNumber = phoneNumber;
        this.location = location;
        return renamed;
    }

    /**
     * Replaces the photo. The storage keeps the image; the profile keeps its description.
     */
    public void changePhoto(PhotoImage image, Instant uploadedAt) {
        Objects.requireNonNull(image, "The photo is required");
        this.photo = ProfilePhoto.of(image, uploadedAt);
    }

    /**
     * @return true when there was a photo to remove
     */
    public boolean removePhoto() {
        var had = photo != null;
        this.photo = null;
        return had;
    }

    public boolean hasPhoto() {
        return photo != null;
    }

    public boolean isStored() {
        return id != null;
    }

    public String getFullNameValue() {
        return fullName == null ? null : fullName.value();
    }
}
