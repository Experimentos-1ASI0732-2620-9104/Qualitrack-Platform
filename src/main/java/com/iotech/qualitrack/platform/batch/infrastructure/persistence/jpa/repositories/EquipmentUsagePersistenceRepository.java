package com.iotech.qualitrack.platform.batch.infrastructure.persistence.jpa.repositories;

import com.iotech.qualitrack.platform.batch.infrastructure.persistence.jpa.entities.EquipmentUsagePersistenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EquipmentUsagePersistenceRepository extends JpaRepository<EquipmentUsagePersistenceEntity, Long> {
    boolean existsByBatchIdAndEquipmentId(Long batchId, Long equipmentId);

    List<EquipmentUsagePersistenceEntity> findAllByBatchIdOrderByIdAsc(Long batchId);
}
