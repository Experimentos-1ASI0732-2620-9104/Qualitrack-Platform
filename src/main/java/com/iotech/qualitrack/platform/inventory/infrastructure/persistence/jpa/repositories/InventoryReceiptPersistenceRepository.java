package com.iotech.qualitrack.platform.inventory.infrastructure.persistence.jpa.repositories;
import com.iotech.qualitrack.platform.inventory.infrastructure.persistence.jpa.entities.InventoryReceiptEntity;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
import java.util.*;

public interface InventoryReceiptPersistenceRepository extends JpaRepository<InventoryReceiptEntity, Long> {
    List<InventoryReceiptEntity> findAllByLaboratoryIdAndMaterialIdOrderByExpiresOnAscIdAsc(Long laboratoryId, Long materialId);
    Optional<InventoryReceiptEntity> findByLaboratoryIdAndId(Long laboratoryId, Long id);
    @Query("select r from InventoryReceiptEntity r where r.laboratoryId = :lab and r.materialId in "
        + "(select m.id from InventoryMaterialEntity m where m.laboratoryId = :lab and m.environmentId = :environment) "
        + "order by r.expiresOn asc, r.id asc")
    List<InventoryReceiptEntity> findAllInEnvironment(Long lab, Long environment);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from InventoryReceiptEntity r where r.laboratoryId = :lab and r.id = :id")
    Optional<InventoryReceiptEntity> findForUpdate(Long lab, Long id);
    @Query("select count(r) > 0 from InventoryReceiptEntity r where r.laboratoryId = :lab and r.materialId = :material and lower(r.supplier) = lower(:supplier) and lower(r.batchNumber) = lower(:number)")
    boolean existsSupplierReceipt(Long lab, Long material, String supplier, String number);
}
