package com.iotech.qualitrack.platform.inventory.domain.model.commands;

import java.math.BigDecimal;

/**
 * Command to create or update a raw material in the inventory catalog
 * of a laboratory environment.
 *
 * <p>Defines the identification and stock configuration of a raw
 * material, including its code, name, measurement unit and minimum
 * stock threshold.</p>
 *
 * <p>When {@code materialId} is {@code null}, the command requests
 * the registration of a new raw material. Otherwise, it requests
 * an update to the existing material.</p>
 *
 * @param laboratoryId identifier of the laboratory owning the material
 * @param environmentId identifier of the environment containing the material
 * @param materialId identifier of the material to update, or {@code null} to create one
 * @param code unique identification code used for the raw material
 * @param name descriptive name of the raw material
 * @param unit measurement unit used to manage the material's stock
 * @param minimumStock minimum quantity used to identify low-stock conditions
 */
public record SaveRawMaterialCommand(
        Long laboratoryId,
        Long environmentId,
        Long materialId,
        String code,
        String name,
        String unit,
        BigDecimal minimumStock
) { }
