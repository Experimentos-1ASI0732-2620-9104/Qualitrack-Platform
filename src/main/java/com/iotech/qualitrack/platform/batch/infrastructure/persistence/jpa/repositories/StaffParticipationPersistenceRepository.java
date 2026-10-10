package com.iotech.qualitrack.platform.batch.infrastructure.persistence.jpa.repositories;

import com.iotech.qualitrack.platform.batch.infrastructure.persistence.jpa.entities.StaffParticipationPersistenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StaffParticipationPersistenceRepository extends JpaRepository<StaffParticipationPersistenceEntity, Long> {
    boolean existsByBatchIdAndStaffId(Long batchId, Long staffId);

    List<StaffParticipationPersistenceEntity> findAllByBatchIdOrderByIdAsc(Long batchId);
}
