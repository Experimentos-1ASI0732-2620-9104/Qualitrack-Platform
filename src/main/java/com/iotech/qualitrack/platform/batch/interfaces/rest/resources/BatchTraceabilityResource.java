package com.iotech.qualitrack.platform.batch.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

/**
 * Traceability of a product batch: what it consumed, which equipment and staff took part and how it was closed.
 */
@Schema(name = "BatchTraceabilityResponse", description = "Resources that took part in a product batch")
public record BatchTraceabilityResource(
        @Schema(description = "The product batch") BatchResource batch,
        @Schema(description = "The manufactured product") ProductSummaryResource product,
        @Schema(description = "Raw material lots consumed by the batch") List<TracedRawMaterialUsageResource> rawMaterials,
        @Schema(description = "Equipment used in the batch") List<EquipmentUsageResource> equipment,
        @Schema(description = "Staff who took part in the batch") List<StaffParticipationResource> staff,
        @Schema(description = "Release signature, when the batch was released", nullable = true) ReleaseEvidenceResource release,
        @Schema(description = "Rejection record, when the batch was rejected", nullable = true) RejectionEvidenceResource rejection,
        @Schema(description = "Monitored container where the batch is stored, when it has one", nullable = true) BatchContainerResource container
) {
    @Schema(name = "TraceabilityProduct")
    public record ProductSummaryResource(Long id, String code, String name) { }

    @Schema(name = "TraceabilityRawMaterialUsage")
    public record TracedRawMaterialUsageResource(
            @Schema(description = "Usage identifier", example = "1") Long id,
            @Schema(description = "Raw material identifier", example = "5") Long rawMaterialId,
            @Schema(description = "Raw material name", example = "Paracetamol API") String rawMaterialName,
            @Schema(description = "Environment where the raw material is kept; null for pre-Inventory usages", example = "1", nullable = true)
            Long rawMaterialEnvironmentId,
            @Schema(description = "Inventory lot consumed; null for pre-Inventory usages", example = "3", nullable = true) Long inventoryReceiptId,
            @Schema(description = "Consumed quantity", example = "12.5") Double quantityUsed,
            @Schema(description = "Unit", example = "kg") String unit,
            @Schema(description = "Usage time", example = "2026-10-02T15:04:05Z") String usageDate,
            @Schema(description = "Lot stock before the consumption", nullable = true) BigDecimal stockBefore,
            @Schema(description = "Lot stock after the consumption", nullable = true) BigDecimal stockAfter
    ) { }

    @Schema(name = "TraceabilityRelease")
    public record ReleaseEvidenceResource(Long signedByUserId, String signatureHash, String signedAt) { }

    @Schema(name = "TraceabilityRejection")
    public record RejectionEvidenceResource(String rejectionDate, String reason) { }
}
