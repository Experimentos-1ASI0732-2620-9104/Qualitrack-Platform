package com.iotech.qualitrack.platform.inventory.application.commandservices;

import com.iotech.qualitrack.platform.inventory.domain.model.commands.ImportLegacyRawMaterialCommand;
import com.iotech.qualitrack.platform.shared.application.result.ApplicationError;
import com.iotech.qualitrack.platform.shared.application.result.Result;

/** Explicit opening-balance import; historical records remain in Laboratory. */
public interface InventoryImportService {
    /**
     * Imports a legacy raw material once; importing it again returns the same raw material.
     *
     * @return the imported raw material; a missing environment or a record that needs reconciliation is raised as an
     * {@link com.iotech.qualitrack.platform.shared.application.result.ApplicationException} so that the import is
     * rolled back
     */
    Result<Long, ApplicationError> handle(ImportLegacyRawMaterialCommand command);
}
