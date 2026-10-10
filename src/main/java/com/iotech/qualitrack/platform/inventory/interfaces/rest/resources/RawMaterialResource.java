package com.iotech.qualitrack.platform.inventory.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(name = "RawMaterialResponse",
    description = "Raw material with stock derived from its lots; stockStatus is LOW when usable stock is below the minimum stock")
public record RawMaterialResource(Long id, Long laboratoryId, Long environmentId, String code, String name, String unit,
    BigDecimal minimumStock, BigDecimal usableStock, BigDecimal physicalStock, String stockStatus, Long legacyId) { }
