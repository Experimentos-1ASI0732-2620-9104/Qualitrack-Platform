package com.iotech.qualitrack.platform.inventory.interfaces.events;

/**
 * Published when a raw material of an environment is registered or updated in the inventory catalogue.
 *
 * @param created true when the material was registered, false when it was updated
 */
public record RawMaterialSavedIntegrationEvent(Long rawMaterialId, Long laboratoryId, Long environmentId, String code,
        String name, boolean created) { }
