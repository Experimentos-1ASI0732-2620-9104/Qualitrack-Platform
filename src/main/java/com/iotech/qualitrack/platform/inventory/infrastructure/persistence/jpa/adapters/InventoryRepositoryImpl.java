package com.iotech.qualitrack.platform.inventory.infrastructure.persistence.jpa.adapters;

import com.iotech.qualitrack.platform.inventory.domain.model.aggregates.*;
import com.iotech.qualitrack.platform.inventory.domain.model.entities.InventoryMovement;
import com.iotech.qualitrack.platform.inventory.domain.repositories.InventoryRepository;
import com.iotech.qualitrack.platform.inventory.infrastructure.persistence.jpa.assemblers.*;
import com.iotech.qualitrack.platform.inventory.infrastructure.persistence.jpa.entities.InventoryMaterialPersistenceEntity;
import com.iotech.qualitrack.platform.inventory.infrastructure.persistence.jpa.repositories.*;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public class InventoryRepositoryImpl implements InventoryRepository {
    private final InventoryMaterialPersistenceRepository materials;
    private final InventoryReceiptPersistenceRepository receipts;
    private final InventoryMovementPersistenceRepository movements;

    public InventoryRepositoryImpl(InventoryMaterialPersistenceRepository materials,
            InventoryReceiptPersistenceRepository receipts, InventoryMovementPersistenceRepository movements) {
        this.materials = materials;
        this.receipts = receipts;
        this.movements = movements;
    }

    @Override
    public List<RawMaterial> materials(Long laboratoryId) {
        return materials.findAllByLaboratoryIdOrderByName(laboratoryId).stream()
            .map(InventoryMaterialPersistenceAssembler::toDomainFromPersistence).toList();
    }

    @Override
    public List<RawMaterial> materials(Long laboratoryId, Long environmentId) {
        return materials.findAllByLaboratoryIdAndEnvironmentIdOrderByName(laboratoryId, environmentId).stream()
            .map(InventoryMaterialPersistenceAssembler::toDomainFromPersistence).toList();
    }

    @Override
    public Optional<RawMaterial> materialById(Long id) {
        return materials.findById(id).map(InventoryMaterialPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public Optional<RawMaterial> material(Long lab, Long id, boolean lock) {
        return (lock ? materials.findForUpdate(lab, id) : materials.findByLaboratoryIdAndId(lab, id))
            .map(InventoryMaterialPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public RawMaterial saveMaterial(RawMaterial material, Long legacyId) {
        return InventoryMaterialPersistenceAssembler.toDomainFromPersistence(
            materials.save(InventoryMaterialPersistenceAssembler.toPersistenceFromDomain(material, legacyId)));
    }

    @Override
    public Optional<Long> legacyId(Long lab, Long id) {
        return materials.findByLaboratoryIdAndId(lab, id).map(InventoryMaterialPersistenceEntity::getLegacyId);
    }

    @Override
    public Optional<Long> importedMaterial(Long lab, Long id) {
        return materials.findByLaboratoryIdAndLegacyId(lab, id).map(InventoryMaterialPersistenceEntity::getId);
    }

    @Override
    public boolean codeExists(Long lab, String code, Long exceptId) {
        return materials.findIdsByCode(lab, code).stream().anyMatch(id -> !id.equals(exceptId));
    }

    @Override
    public List<RawMaterialBatch> receipts(Long lab, Long material) {
        return receipts.findAllByLaboratoryIdAndMaterialIdOrderByExpiresOnAscIdAsc(lab, material).stream()
            .map(InventoryReceiptPersistenceAssembler::toDomainFromPersistence).toList();
    }

    @Override
    public Optional<RawMaterialBatch> receipt(Long lab, Long id, boolean lock) {
        return (lock ? receipts.findForUpdate(lab, id) : receipts.findByLaboratoryIdAndId(lab, id))
            .map(InventoryReceiptPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public Optional<RawMaterialBatch> receiptById(Long id) {
        return receipts.findById(id).map(InventoryReceiptPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public List<RawMaterialBatch> environmentReceipts(Long lab, Long environment) {
        return receipts.findAllInEnvironment(lab, environment).stream()
            .map(InventoryReceiptPersistenceAssembler::toDomainFromPersistence).toList();
    }

    @Override
    public RawMaterialBatch saveReceipt(RawMaterialBatch receipt) {
        return InventoryReceiptPersistenceAssembler.toDomainFromPersistence(
            receipts.save(InventoryReceiptPersistenceAssembler.toPersistenceFromDomain(receipt)));
    }

    @Override
    public boolean receiptExists(Long lab, Long material, String supplier, String number) {
        return receipts.existsSupplierReceipt(lab, material, supplier, number);
    }

    @Override
    public List<InventoryMovement> movements(Long lab, Long material) {
        return movements.findAllByLaboratoryIdAndMaterialIdOrderByOccurredAtDescIdDesc(lab, material).stream()
            .map(InventoryMovementPersistenceAssembler::toDomainFromPersistence).toList();
    }

    @Override
    public Optional<InventoryMovement> operation(Long lab, String operationId) {
        return movements.findByLaboratoryIdAndOperationId(lab, operationId)
            .map(InventoryMovementPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public InventoryMovement append(InventoryMovement movement) {
        return InventoryMovementPersistenceAssembler.toDomainFromPersistence(
            movements.save(InventoryMovementPersistenceAssembler.toPersistenceFromDomain(movement)));
    }
}
