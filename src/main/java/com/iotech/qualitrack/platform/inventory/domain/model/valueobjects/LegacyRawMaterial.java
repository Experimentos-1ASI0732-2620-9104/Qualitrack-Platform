package com.iotech.qualitrack.platform.inventory.domain.model.valueobjects;

import java.math.BigDecimal;

/**
 * Raw material recorded by Laboratory Management before Inventory existed, whose opening balance can be imported into
 * an environment.
 *
 * @param id           the legacy raw material identifier
 * @param minimumStock the minimum stock of the material
 * @param supplier     the supplier of the balance
 * @param batchNumber  the supplier batch number of the balance
 * @param expiresOn    the expiry date as it was recorded, ISO-8601 when valid
 * @param balance      the current stock, which becomes the opening balance
 */
public record LegacyRawMaterial(Long id, String code, String name, String unit, BigDecimal minimumStock,
                                String supplier, String batchNumber, String expiresOn, BigDecimal balance) { }
