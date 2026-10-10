package com.iotech.qualitrack.platform.inventory.application.commandservices;

import com.iotech.qualitrack.platform.inventory.domain.model.commands.ImportLegacyRawMaterialCommand;

/** Explicit opening-balance import; historical records remain in Laboratory. */
public interface InventoryImportService {
    Long handle(ImportLegacyRawMaterialCommand command);
}
