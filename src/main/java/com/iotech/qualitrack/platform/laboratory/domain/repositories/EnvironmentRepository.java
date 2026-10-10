package com.iotech.qualitrack.platform.laboratory.domain.repositories;

import com.iotech.qualitrack.platform.laboratory.domain.model.aggregates.Environment;

import java.util.List;
import java.util.Optional;

/**
 * Environment repository port.
 */
public interface EnvironmentRepository {
    Optional<Environment> findById(Long id);

    Optional<Environment> findByIdAndLaboratoryId(Long id, Long laboratoryId);

    List<Environment> findAllByLaboratoryId(Long laboratoryId);

    boolean existsByLaboratoryIdAndCode(Long laboratoryId, String code);

    boolean existsByLaboratoryIdAndCodeAndIdNot(Long laboratoryId, String code, Long id);

    Environment save(Environment environment);
}
