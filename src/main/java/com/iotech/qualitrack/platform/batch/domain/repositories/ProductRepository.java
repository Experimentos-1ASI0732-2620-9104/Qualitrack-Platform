package com.iotech.qualitrack.platform.batch.domain.repositories;

import com.iotech.qualitrack.platform.batch.domain.model.aggregates.PharmaceuticalProduct;

import java.util.List;
import java.util.Optional;

/**
 * Repository of pharmaceutical products.
 */
public interface ProductRepository {
    Optional<PharmaceuticalProduct> findById(Long id);

    List<PharmaceuticalProduct> findAllByLaboratoryIdAndEnvironmentId(Long laboratoryId, Long environmentId);

    Optional<PharmaceuticalProduct> findByNameAndLaboratoryId(String name, Long laboratoryId);

    boolean existsByLaboratoryIdAndCode(Long laboratoryId, String code);

    PharmaceuticalProduct save(PharmaceuticalProduct product);
}
