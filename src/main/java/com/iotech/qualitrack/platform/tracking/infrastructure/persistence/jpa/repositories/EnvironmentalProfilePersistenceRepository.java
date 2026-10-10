package com.iotech.qualitrack.platform.tracking.infrastructure.persistence.jpa.repositories;

import com.iotech.qualitrack.platform.tracking.domain.model.valueobjects.ProfileScope;
import com.iotech.qualitrack.platform.tracking.infrastructure.persistence.jpa.entities.EnvironmentalProfilePersistenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EnvironmentalProfilePersistenceRepository
        extends JpaRepository<EnvironmentalProfilePersistenceEntity, Long> {

    Optional<EnvironmentalProfilePersistenceEntity> findByScopeAndEnvironmentId(ProfileScope scope, Long environmentId);

    Optional<EnvironmentalProfilePersistenceEntity> findByScopeAndDeviceId(ProfileScope scope, Long deviceId);
}
