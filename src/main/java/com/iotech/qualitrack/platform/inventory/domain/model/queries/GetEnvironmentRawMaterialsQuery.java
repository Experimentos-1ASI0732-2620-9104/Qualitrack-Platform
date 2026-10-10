package com.iotech.qualitrack.platform.inventory.domain.model.queries;

import com.iotech.qualitrack.platform.inventory.domain.model.valueobjects.StockStatus;

/**
 * Raw materials kept in an environment, optionally filtered by stock status (TS22, TS27).
 */
public record GetEnvironmentRawMaterialsQuery(Long laboratoryId, Long environmentId, StockStatus stockStatus) { }
