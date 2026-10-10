package com.iotech.qualitrack.platform.inventory.domain.model.queries;

import com.iotech.qualitrack.platform.inventory.domain.model.valueobjects.ExpirationStatus;

/**
 * Lots of all raw materials kept in an environment, optionally filtered by expiration status (TS28).
 *
 * @param withinDays near expiry period in days; null uses the configured default
 */
public record GetEnvironmentRawMaterialBatchesQuery(Long laboratoryId, Long environmentId,
                                                    ExpirationStatus expirationStatus, Integer withinDays) { }
