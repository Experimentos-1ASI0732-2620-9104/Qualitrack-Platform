package com.iotech.qualitrack.platform.inventory.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(name = "RawMaterialBatchReviewResponse", description = "Quality review registered for a raw material lot")
public record RawMaterialBatchReviewResource(Long rawMaterialBatchId, Long rawMaterialId, String previousStatus,
    String status, String reason, Long reviewedBy, Instant reviewedAt) { }
