package com.iotech.qualitrack.platform.profile.infrastructure.persistence.jpa.repositories;

import com.iotech.qualitrack.platform.profile.infrastructure.persistence.jpa.entities.ProfilePersistenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProfilePersistenceRepository extends JpaRepository<ProfilePersistenceEntity, Long> {

    Optional<ProfilePersistenceEntity> findByUserId(Long userId);
}
