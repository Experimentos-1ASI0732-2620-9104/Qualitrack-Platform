package com.iotech.qualitrack.platform.batch.domain.repositories;

import com.iotech.qualitrack.platform.batch.domain.model.entities.EquipmentUsage;
import com.iotech.qualitrack.platform.batch.domain.model.entities.StaffParticipation;

import java.util.List;

/**
 * Stores the equipment and staff that took part in product batches.
 */
public interface BatchParticipationRepository {
    EquipmentUsage saveEquipmentUsage(EquipmentUsage usage);

    StaffParticipation saveStaffParticipation(StaffParticipation participation);

    boolean existsEquipmentUsage(Long batchId, Long equipmentId);

    boolean existsStaffParticipation(Long batchId, Long staffId);

    List<EquipmentUsage> findEquipmentUsagesByBatchId(Long batchId);

    List<StaffParticipation> findStaffParticipationsByBatchId(Long batchId);
}
