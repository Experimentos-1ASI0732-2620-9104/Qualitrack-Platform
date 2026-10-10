package com.iotech.qualitrack.platform.batch.infrastructure.persistence.jpa.repositories;

import com.iotech.qualitrack.platform.batch.infrastructure.persistence.jpa.entities.ProductPersistenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data repository of pharmaceutical products.
 */
@Repository
public interface ProductPersistenceRepository extends JpaRepository<ProductPersistenceEntity, Long> {
    List<ProductPersistenceEntity> findAllByLaboratoryIdAndEnvironmentIdOrderByNameAsc(Long laboratoryId, Long environmentId);

    Optional<ProductPersistenceEntity> findByNameAndLaboratoryId(String name, Long laboratoryId);

    boolean existsByLaboratoryIdAndCode(Long laboratoryId, String code);
}
