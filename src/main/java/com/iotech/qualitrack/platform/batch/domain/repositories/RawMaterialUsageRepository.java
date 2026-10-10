package com.iotech.qualitrack.platform.batch.domain.repositories;

import com.iotech.qualitrack.platform.batch.domain.model.entities.RawMaterialUsage;

import java.util.List;
import java.util.Optional;

/**
 * Raw material usage repository port.
 *
 * <p>Handles the persistence contract for raw material consumption records
 * linked to production batches.</p>
 */
public interface RawMaterialUsageRepository {

    Optional<RawMaterialUsage> findById(Long id);

    List<RawMaterialUsage> findAll();

    List<RawMaterialUsage> findAllByBatchId(Long batchId);

    List<RawMaterialUsage> findAllByRawMaterialId(Long rawMaterialId);

    Optional<RawMaterialUsage> findByBatchIdAndOperationId(Long batchId, String operationId);

    RawMaterialUsage save(RawMaterialUsage rawMaterialUsage);

    boolean existsById(Long id);

    void deleteById(Long id);
}