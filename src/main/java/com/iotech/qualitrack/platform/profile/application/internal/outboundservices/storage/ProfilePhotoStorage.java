package com.iotech.qualitrack.platform.profile.application.internal.outboundservices.storage;

import com.iotech.qualitrack.platform.profile.domain.model.valueobjects.PhotoImage;

import java.util.Optional;

/**
 * Keeps the images of the profile photos. The current implementation stores them in the database, which needs no
 * extra service and survives the redeploys of hosts without a persistent disk.
 */
public interface ProfilePhotoStorage {

    void store(Long profileId, PhotoImage image);

    Optional<PhotoImage> find(Long profileId);

    void remove(Long profileId);
}
