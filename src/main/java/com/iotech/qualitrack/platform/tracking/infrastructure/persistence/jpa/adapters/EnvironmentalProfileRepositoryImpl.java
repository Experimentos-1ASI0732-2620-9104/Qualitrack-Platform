package com.iotech.qualitrack.platform.tracking.infrastructure.persistence.jpa.adapters;

import com.iotech.qualitrack.platform.tracking.domain.model.aggregates.EnvironmentalProfile;
import com.iotech.qualitrack.platform.tracking.domain.model.valueobjects.ProfileScope;
import com.iotech.qualitrack.platform.tracking.domain.repositories.EnvironmentalProfileRepository;
import com.iotech.qualitrack.platform.tracking.infrastructure.persistence.jpa.assemblers.EnvironmentalProfilePersistenceAssembler;
import com.iotech.qualitrack.platform.tracking.infrastructure.persistence.jpa.repositories.EnvironmentalProfilePersistenceRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class EnvironmentalProfileRepositoryImpl implements EnvironmentalProfileRepository {
    private final EnvironmentalProfilePersistenceRepository repository;

    public EnvironmentalProfileRepositoryImpl(EnvironmentalProfilePersistenceRepository repository) {
        this.repository = repository;
    }

    @Override
    public EnvironmentalProfile save(EnvironmentalProfile profile) {
        var existing = profile.getId() == null ? null : repository.findById(profile.getId()).orElse(null);
        var saved = repository.save(EnvironmentalProfilePersistenceAssembler.toPersistenceFromDomain(profile, existing));
        return EnvironmentalProfilePersistenceAssembler.toDomainFromPersistence(saved);
    }

    @Override
    public Optional<EnvironmentalProfile> findByEnvironmentId(Long environmentId) {
        return repository.findByScopeAndEnvironmentId(ProfileScope.ENVIRONMENT, environmentId)
                .map(EnvironmentalProfilePersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public Optional<EnvironmentalProfile> findByDeviceId(Long deviceId) {
        return repository.findByScopeAndDeviceId(ProfileScope.CONTAINER_MONITOR, deviceId)
                .map(EnvironmentalProfilePersistenceAssembler::toDomainFromPersistence);
    }
}
