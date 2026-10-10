package com.iotech.qualitrack.platform.laboratory.infrastructure.persistence.jpa.repositories;

import com.iotech.qualitrack.platform.laboratory.infrastructure.persistence.jpa.entities.StaffPersistenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link StaffPersistenceEntity}.
 *
 * <p>Handles database operations for the staff members table using Long as the primary key.
 * Provides custom queries for fetching staff by laboratory and validating unique emails.</p>
 */
@Repository
public interface StaffPersistenceRepository extends JpaRepository<StaffPersistenceEntity, Long> {

    /**
     * Finds all staff members associated with a specific laboratory.
     * @param laboratoryId The numeric ID of the laboratory.
     */
    List<StaffPersistenceEntity> findAllByLaboratoryId(Long laboratoryId);

    /**
     * Finds a staff member by their unique institutional email address.
     * @param email The unique email address.
     */
    Optional<StaffPersistenceEntity> findByEmail(String email);

    /**
     * Checks if a staff member with the given email already exists.
     * Useful for Fail-Fast validation during staff registration to prevent duplicate accounts.
     */
    boolean existsByEmail(String email);

    Optional<StaffPersistenceEntity> findByUserId(Long userId);
}