package com.iotech.qualitrack.platform.inventory.infrastructure.persistence.jpa.repositories;

import com.iotech.qualitrack.platform.inventory.infrastructure.persistence.jpa.entities.InventoryReceiptPersistenceEntity;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface InventoryReceiptPersistenceRepository extends JpaRepository<InventoryReceiptPersistenceEntity, Long> {
    List<InventoryReceiptPersistenceEntity> findAllByLaboratoryIdAndMaterialIdOrderByExpiresOnAscIdAsc(Long laboratoryId, Long materialId);
    Optional<InventoryReceiptPersistenceEntity> findByLaboratoryIdAndId(Long laboratoryId, Long id);
    @Query("select r from InventoryReceiptPersistenceEntity r where r.laboratoryId = :lab and r.materialId in "
        + "(select m.id from InventoryMaterialPersistenceEntity m where m.laboratoryId = :lab and m.environmentId = :environment) "
        + "order by r.expiresOn asc, r.id asc")
    List<InventoryReceiptPersistenceEntity> findAllInEnvironment(Long lab, Long environment);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from InventoryReceiptPersistenceEntity r where r.laboratoryId = :lab and r.id = :id")
    Optional<InventoryReceiptPersistenceEntity> findForUpdate(Long lab, Long id);
    @Query("select count(r) > 0 from InventoryReceiptPersistenceEntity r where r.laboratoryId = :lab and r.materialId = :material and lower(r.supplier) = lower(:supplier) and lower(r.batchNumber) = lower(:number)")
    boolean existsSupplierReceipt(Long lab, Long material, String supplier, String number);
}
