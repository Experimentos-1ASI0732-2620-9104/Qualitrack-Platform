package com.iotech.qualitrack.platform.inventory.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(name = "RawMaterialStockResponse",
    description = "Usable stock is the sum of available amounts of RELEASED, non-expired lots; physical stock includes every lot")
public record RawMaterialStockResource(Long rawMaterialId, String unit, BigDecimal usableStock, BigDecimal physicalStock,
    BigDecimal minimumStock, String stockStatus) { }
