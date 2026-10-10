package com.iotech.qualitrack.platform.inventory.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(name = "RawMaterialBatchResponse",
    description = "Raw material lot (supplier receipt) with its review status and expiration classification")
public record ReceiptResource(Long id, Long laboratoryId, Long rawMaterialId, String supplier,
        String batchNumber, String unit, BigDecimal initialAmount, BigDecimal availableAmount,
        LocalDate receivedOn, LocalDate expiresOn, String status, boolean usable, String availability,
        String expirationStatus, Long containerMonitorId) { }
