package com.iotech.qualitrack.platform.profile.infrastructure.persistence.jpa.repositories;

import com.iotech.qualitrack.platform.profile.infrastructure.persistence.jpa.entities.ProfilePhotoPersistenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProfilePhotoPersistenceRepository extends JpaRepository<ProfilePhotoPersistenceEntity, Long> {

    Optional<ProfilePhotoPersistenceEntity> findByProfileId(Long profileId);

    void deleteByProfileId(Long profileId);
}
