package com.iotech.qualitrack.platform.batch.infrastructure.persistence.jpa.repositories;

import com.iotech.qualitrack.platform.batch.infrastructure.persistence.jpa.entities.RawMaterialUsagePersistenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for {@link RawMaterialUsagePersistenceEntity}.
 *
 * <p>Handles database operations for raw material usage records linked to batches.</p>
 */
@Repository
public interface RawMaterialUsagePersistenceRepository extends JpaRepository<RawMaterialUsagePersistenceEntity, Long> {

    /**
     * Finds all raw material usage records associated with a specific batch.
     * @param batchId The numeric ID of the batch.
     */
    List<RawMaterialUsagePersistenceEntity> findAllByBatchId(Long batchId);

    /**
     * Finds all usage records associated with a specific raw material.
     * @param rawMaterialId The numeric ID of the raw material.
     */
    List<RawMaterialUsagePersistenceEntity> findAllByRawMaterialId(Long rawMaterialId);

    java.util.Optional<RawMaterialUsagePersistenceEntity> findByBatchIdAndOperationId(Long batchId, String operationId);
}