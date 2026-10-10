package com.iotech.qualitrack.platform.inventory.domain.model.valueobjects;

import java.time.Instant;

/**
 * Result of the quality review of a raw material lot (US39): status transition, reason and reviewer.
 */
public record RawMaterialBatchReview(Long rawMaterialBatchId, Long rawMaterialId, String previousStatus,
    String status, String reason, Long reviewedBy, Instant reviewedAt) { }
