package com.iotech.qualitrack.platform.tracking.domain.repositories;

import com.iotech.qualitrack.platform.tracking.domain.model.aggregates.EnvironmentalProfile;

import java.util.Optional;

/**
 * Repository of the environmental profiles.
 */
public interface EnvironmentalProfileRepository {
    EnvironmentalProfile save(EnvironmentalProfile profile);

    /**
     * Profile of an environment (ENVIRONMENT scope).
     */
    Optional<EnvironmentalProfile> findByEnvironmentId(Long environmentId);

    /**
     * Profile of a container monitor (CONTAINER_MONITOR scope).
     */
    Optional<EnvironmentalProfile> findByDeviceId(Long deviceId);
}
