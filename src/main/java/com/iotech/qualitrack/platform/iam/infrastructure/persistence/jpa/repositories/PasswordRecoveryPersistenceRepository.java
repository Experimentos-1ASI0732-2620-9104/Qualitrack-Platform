package com.iotech.qualitrack.platform.iam.infrastructure.persistence.jpa.repositories;

import com.iotech.qualitrack.platform.iam.infrastructure.persistence.jpa.entities.PasswordRecoveryPersistenceEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository of the password recoveries.
 */
@Repository
public interface PasswordRecoveryPersistenceRepository extends JpaRepository<PasswordRecoveryPersistenceEntity, Long> {

    Optional<PasswordRecoveryPersistenceEntity> findFirstByUserIdOrderByRequestedAtDescIdDesc(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PasswordRecoveryPersistenceEntity> findFirstWithLockByUserIdOrderByRequestedAtDescIdDesc(Long userId);
}
