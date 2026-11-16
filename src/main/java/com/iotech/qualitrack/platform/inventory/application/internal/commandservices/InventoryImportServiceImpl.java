package com.iotech.qualitrack.platform.inventory.application.internal.commandservices;

import com.iotech.qualitrack.platform.inventory.application.internal.outboundservices.acl.InventoryExternalLaboratoryService;
import com.iotech.qualitrack.platform.inventory.domain.model.commands.ImportLegacyRawMaterialCommand;
import com.iotech.qualitrack.platform.inventory.application.commandservices.InventoryImportService;

import com.iotech.qualitrack.platform.inventory.domain.model.aggregates.RawMaterial;
import com.iotech.qualitrack.platform.inventory.domain.model.aggregates.RawMaterialBatch;
import com.iotech.qualitrack.platform.inventory.domain.repositories.InventoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import com.iotech.qualitrack.platform.inventory.application.internal.outboundservices.InventoryMovementRecorder;
import com.iotech.qualitrack.platform.shared.application.result.ApplicationError;
import com.iotech.qualitrack.platform.shared.application.result.ApplicationException;
import com.iotech.qualitrack.platform.shared.application.result.Result;


/**
 * Application service responsible for importing legacy raw materials
 * into the current Inventory bounded context.
 *
 * <p>Coordinates the migration of existing laboratory materials
 * into the inventory model based on raw materials and supplier batches.</p>
 *
 * @see InventoryImportService
 */
@Service
@Transactional
public class InventoryImportServiceImpl implements InventoryImportService {
    private final InventoryRepository repository;
    private final InventoryMovementRecorder recorder;
    private final Clock clock;
    private final InventoryExternalLaboratoryService laboratories;
    public InventoryImportServiceImpl(InventoryRepository repository, InventoryMovementRecorder recorder,
            Clock inventoryClock, InventoryExternalLaboratoryService laboratories) {
        this.repository = repository;
        this.laboratories = laboratories;
        this.recorder = recorder;
        this.clock = inventoryClock;
    }

    /**
     * Processes the import of a legacy raw material into Inventory.
     *
     * <p>Uses the legacy material information associated with the
     * requested laboratory and coordinates the registration of the
     * corresponding inventory records.</p>
     *
     * @param command command identifying the legacy material to import
     * @return the result of the import operation
     */
    @Override
    public Result<Long, ApplicationError> handle(ImportLegacyRawMaterialCommand command) {
        var lab = command.laboratoryId();
        var legacyId = command.legacyId();
        if (!laboratories.existsEnvironment(lab, command.environmentId()))
            throw new ApplicationException(ApplicationError.notFound("Environment", command.environmentId()));
        var previous = laboratories.lockLegacyMaterial(lab, legacyId);
        var imported = repository.importedMaterial(lab, legacyId);
        if (imported.isPresent()) return Result.success(imported.get());
        if (repository.codeExists(lab, previous.code(), null)) throw conflict("Material code already exists; review the legacy record before importing");
        var material = repository.saveMaterial(new RawMaterial(null, lab, command.environmentId(), previous.code(), previous.name(),
            previous.unit(), previous.minimumStock()), legacyId);
        if (previous.balance().signum() < 0) throw conflict("Legacy balance requires reconciliation");
        if (previous.balance().signum() > 0) {
            LocalDate expires;
            try {
                expires = LocalDate.parse(previous.expiresOn());
            } catch (DateTimeParseException | NullPointerException error) {
                throw conflict("Legacy expiry date requires reconciliation before import");
            }
            // The opening date is the import date (or expiry for an already expired balance), not a fabricated original receipt date.
            LocalDate importDate = LocalDate.now(clock);
            var openingDate = expires.isBefore(importDate) ? expires : importDate;
            var receipt = repository.saveReceipt(RawMaterialBatch.receive(lab, material.getId(), previous.supplier(),
                previous.batchNumber(), previous.unit(), previous.balance(), openingDate, expires));
            recorder.record(receipt, null, "OPENING_BALANCE", previous.balance(), BigDecimal.ZERO, null,
                "Imported legacy balance #" + legacyId + "; original reception date unknown; requires quality review", null);
        }
        return Result.success(material.getId());
    }

    private static ApplicationException conflict(String message) {
        return new ApplicationException(ApplicationError.conflict("Inventory", message));
    }
}
