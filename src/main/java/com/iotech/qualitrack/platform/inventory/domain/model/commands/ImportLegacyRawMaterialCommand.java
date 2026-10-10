package com.iotech.qualitrack.platform.inventory.domain.model.commands;

/**
 * Imports a pre-Inventory raw material record and its opening balance into an environment.
 */
public record ImportLegacyRawMaterialCommand(Long laboratoryId, Long environmentId, Long legacyId) { }
