package com.iotech.qualitrack.platform.inventory.infrastructure.persistence.jpa.repositories;
import com.iotech.qualitrack.platform.inventory.infrastructure.persistence.jpa.entities.InventoryMaterialEntity;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
import java.util.*;

public interface InventoryMaterialPersistenceRepository extends JpaRepository<InventoryMaterialEntity, Long> {
    List<InventoryMaterialEntity> findAllByLaboratoryIdOrderByName(Long laboratoryId);
    List<InventoryMaterialEntity> findAllByLaboratoryIdAndEnvironmentIdOrderByName(Long laboratoryId, Long environmentId);
    Optional<InventoryMaterialEntity> findByLaboratoryIdAndId(Long laboratoryId, Long id);
    Optional<InventoryMaterialEntity> findByLaboratoryIdAndLegacyId(Long laboratoryId, Long legacyId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from InventoryMaterialEntity m where m.laboratoryId = :lab and m.id = :id")
    Optional<InventoryMaterialEntity> findForUpdate(Long lab, Long id);
    @Query("select m.id from InventoryMaterialEntity m where m.laboratoryId = :lab and lower(m.code) = lower(:code)")
    List<Long> findIdsByCode(Long lab, String code);
}
