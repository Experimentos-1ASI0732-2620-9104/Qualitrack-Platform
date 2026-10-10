package com.iotech.qualitrack.platform.laboratory.infrastructure.persistence.jpa.repositories;

import com.iotech.qualitrack.platform.laboratory.infrastructure.persistence.jpa.entities.EnvironmentPersistenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link EnvironmentPersistenceEntity}.
 */
@Repository
public interface EnvironmentPersistenceRepository extends JpaRepository<EnvironmentPersistenceEntity, Long> {

    Optional<EnvironmentPersistenceEntity> findByIdAndLaboratoryId(Long id, Long laboratoryId);

    List<EnvironmentPersistenceEntity> findAllByLaboratoryId(Long laboratoryId);

    boolean existsByLaboratoryIdAndCode(Long laboratoryId, String code);

    boolean existsByLaboratoryIdAndCodeAndIdNot(Long laboratoryId, String code, Long id);
}
