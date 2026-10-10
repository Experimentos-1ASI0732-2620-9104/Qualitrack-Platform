package com.iotech.qualitrack.platform.batch.infrastructure.persistence.jpa.repositories;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import com.iotech.qualitrack.platform.batch.infrastructure.persistence.jpa.entities.BatchPersistenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link BatchPersistenceEntity}.
 *
 * <p>Handles database operations for the batches table using Long as the primary key.
 * Provides custom queries for fetching batches by laboratory, product and batch number.</p>
 */
@Repository
public interface BatchPersistenceRepository extends JpaRepository<BatchPersistenceEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from BatchPersistenceEntity b where b.id = :id")
    Optional<BatchPersistenceEntity> findByIdForUpdate(Long id);

    /**
     * Finds all batches associated with a specific laboratory.
     * @param labId The numeric ID of the laboratory.
     */
    List<BatchPersistenceEntity> findAllByLabId(Long labId);

    /**
     * Finds the batches of a product, newest start date first.
     * @param productId The numeric ID of the product.
     */
    List<BatchPersistenceEntity> findAllByProductIdOrderByStartDateDescIdDesc(Long productId);

    /**
     * Checks whether the laboratory already uses the batch number.
     */
    boolean existsByLabIdAndBatchNumber(Long labId, String batchNumber);
}
