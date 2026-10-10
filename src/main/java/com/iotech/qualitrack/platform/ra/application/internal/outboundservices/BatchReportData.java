package com.iotech.qualitrack.platform.ra.application.internal.outboundservices;

import java.time.Instant;
import java.util.List;

/** Immutable, presentation-neutral snapshot of records linked to one production batch (US96). */
public record BatchReportData(
        String laboratory, Long batchId, String batchNumber, String product,
        Double quantity, String unit, String status, String started, String finished, String notes,
        List<Material> materials, List<Equipment> equipment, List<Staff> staff, Container container,
        Release release, Rejection rejection, boolean deviationsIncluded, List<Deviation> deviations,
        String generatedBy, Instant generatedAt
) {
    public BatchReportData {
        materials = List.copyOf(materials);
        equipment = List.copyOf(equipment);
        staff = List.copyOf(staff);
        deviations = List.copyOf(deviations);
    }

    /** Raw material consumed; lotId and stock are null for usages recorded before Inventory Management. */
    public record Material(Long id, String name, Long lotId, Double quantity, String unit, String usedOn,
                           String stockBefore, String stockAfter) {}

    public record Equipment(Long id, String name, String registeredAt) {}

    public record Staff(Long id, String name, String role, String registeredAt) {}

    public record Container(Long id, String name, Long environmentId, String assignedAt) {}

    public record Release(Long signedBy, String signatureHash, String signedAt) {}

    public record Rejection(String rejectionDate, String reason) {}

    public record Deviation(Long id, Long equipmentId, String parameter, Double value,
                            Double threshold, String unit, String recordedAt, String severity,
                            String status, String resolution) {}
}
