package com.iotech.qualitrack.platform.inventory.application.internal.commandservices;

import com.iotech.qualitrack.platform.inventory.application.internal.outboundservices.acl.InventoryExternalLaboratoryService;
import com.iotech.qualitrack.platform.inventory.domain.model.commands.ImportLegacyRawMaterialCommand;
import com.iotech.qualitrack.platform.inventory.application.commandservices.InventoryImportService;

import com.iotech.qualitrack.platform.inventory.domain.model.aggregates.*;
import com.iotech.qualitrack.platform.inventory.domain.repositories.InventoryRepository;
import com.iotech.qualitrack.platform.laboratory.interfaces.acl.LegacyInventoryFacade;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Clock;
import com.iotech.qualitrack.platform.inventory.application.internal.outboundservices.InventoryMovementRecorder;
import com.iotech.qualitrack.platform.shared.application.result.*;

@Service
@Transactional
public class InventoryImportServiceImpl implements InventoryImportService {
    private final InventoryRepository repository;
    private final LegacyInventoryFacade legacy;
    private final InventoryMovementRecorder recorder;
    private final Clock clock;
    private final InventoryExternalLaboratoryService laboratories;
    public InventoryImportServiceImpl(InventoryRepository repository, LegacyInventoryFacade legacy, InventoryMovementRecorder recorder,
            Clock inventoryClock, InventoryExternalLaboratoryService laboratories) {
        this.repository = repository;
        this.laboratories = laboratories;
        this.legacy = legacy;
        this.recorder = recorder;
        this.clock = inventoryClock;
    }

    public Long handle(ImportLegacyRawMaterialCommand command) {
        var lab = command.laboratoryId();
        var legacyId = command.legacyId();
        if (!laboratories.existsEnvironment(lab, command.environmentId()))
            throw new ApplicationException(ApplicationError.notFound("Environment", command.environmentId()));
        var previous = legacy.lock(lab, legacyId);
        var imported = repository.importedMaterial(lab, legacyId);
        if (imported.isPresent()) return imported.get();
        if (repository.codeExists(lab, previous.code(), null)) throw conflict("Material code already exists; review the legacy record before importing");
        var material = repository.saveMaterial(new RawMaterial(null, lab, command.environmentId(), previous.code(), previous.name(),
            previous.unit(), previous.minimumStock()), legacyId);
        if (previous.balance().signum() < 0) throw conflict("Legacy balance requires reconciliation");
        if (previous.balance().signum() > 0) {
            LocalDate expires;
            try {
                expires = LocalDate.parse(previous.expiresOn());
            } catch (java.time.format.DateTimeParseException | NullPointerException error) {
                throw conflict("Legacy expiry date requires reconciliation before import");
            }
            // The opening date is the import date (or expiry for an already expired balance), not a fabricated original receipt date.
            var openingDate = expires.isBefore(LocalDate.now(clock)) ? expires : LocalDate.now(clock);
            var receipt = repository.saveReceipt(RawMaterialBatch.receive(lab, material.getId(), previous.supplier(),
                previous.batchNumber(), previous.unit(), previous.balance(), openingDate, expires));
            recorder.record(receipt, null, "OPENING_BALANCE", previous.balance(), BigDecimal.ZERO, null,
                "Imported legacy balance #" + legacyId + "; original reception date unknown; requires quality review", null);
        }
        return material.getId();
    }

    private static ApplicationException conflict(String message) {
        return new ApplicationException(ApplicationError.conflict("Inventory", message));
    }
}
