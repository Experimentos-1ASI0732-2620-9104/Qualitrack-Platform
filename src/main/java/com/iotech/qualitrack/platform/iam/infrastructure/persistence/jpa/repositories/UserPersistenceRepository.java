package com.iotech.qualitrack.platform.iam.infrastructure.persistence.jpa.repositories;

import com.iotech.qualitrack.platform.iam.infrastructure.persistence.jpa.entities.UserPersistenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for IAM user persistence entities.
 */
@Repository
public interface UserPersistenceRepository extends JpaRepository<UserPersistenceEntity, Long> {

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select u from UserPersistenceEntity u where u.id = :id")
    Optional<UserPersistenceEntity> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") Long id);

    Optional<UserPersistenceEntity> findByUsername(String username);

    boolean existsByUsername(String username);

    Optional<UserPersistenceEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    List<UserPersistenceEntity> findByLaboratoryId(Long laboratoryId);
}
