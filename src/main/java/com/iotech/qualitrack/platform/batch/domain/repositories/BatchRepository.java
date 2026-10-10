package com.iotech.qualitrack.platform.batch.domain.repositories;

import com.iotech.qualitrack.platform.batch.domain.model.aggregates.Batch;

import java.util.List;
import java.util.Optional;

/**
 * Batch repository port.
 *
 * <p>Handles the persistence contract for the Batch aggregate.</p>
 */
public interface BatchRepository {

    Optional<Batch> findById(Long id);

    Optional<Batch> findByIdForUpdate(Long id);

    List<Batch> findAllByLabId(Long labId);

    List<Batch> findAllByProductId(Long productId);

    Batch save(Batch batch);

    boolean existsById(Long id);

    boolean existsByLabIdAndBatchNumber(Long labId, String batchNumber);
}
