package com.iotech.qualitrack.platform.iam.infrastructure.persistence.jpa.repositories;

import com.iotech.qualitrack.platform.iam.infrastructure.persistence.jpa.entities.PasswordRecoveryPersistenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository of the password recoveries.
 */
@Repository
public interface PasswordRecoveryPersistenceRepository extends JpaRepository<PasswordRecoveryPersistenceEntity, Long> {

    Optional<PasswordRecoveryPersistenceEntity> findFirstByUserIdOrderByRequestedAtDescIdDesc(Long userId);
}
