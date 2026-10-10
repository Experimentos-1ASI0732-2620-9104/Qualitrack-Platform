package com.iotech.qualitrack.platform.ra.application.internal.outboundservices;

import java.time.Instant;
import java.util.List;

/**
 * Snapshot of the raw materials, lots and quantities of a laboratory on a business date (US97).
 */
public record InventoryReportData(Long laboratoryId, String laboratory, String businessDate,
                                  List<EnvironmentInventory> environments, String generatedBy, Instant generatedAt) {
    public InventoryReportData { environments = List.copyOf(environments); }

    /** Raw materials of one environment. */
    public record EnvironmentInventory(Long id, String code, String name, List<Material> materials) {
        public EnvironmentInventory { materials = List.copyOf(materials); }
    }

    /** Raw material with its stock and lots. */
    public record Material(Long id, String code, String name, String unit, String minimumStock, String usableStock,
                           String physicalStock, String stockStatus, List<Lot> lots) {
        public Material { lots = List.copyOf(lots); }
    }

    /** Supplier lot with its quantities, quality status, expiration and container. */
    public record Lot(Long id, String supplier, String batchNumber, String initialAmount, String availableAmount,
                      String receivedOn, String expiresOn, String status, String expirationStatus, String container) {}

    /** Raw materials of every environment. */
    public int materials() { return environments.stream().mapToInt(item -> item.materials().size()).sum(); }

    /** Lots of every environment. */
    public int lots() { return environments.stream().flatMap(item -> item.materials().stream()).mapToInt(item -> item.lots().size()).sum(); }

    /** Raw materials below their minimum stock. */
    public long lowStock() {
        return environments.stream().flatMap(item -> item.materials().stream()).filter(item -> "LOW".equals(item.stockStatus())).count();
    }

    /** Lots that expired or are near expiry. */
    public long expiring() {
        return environments.stream().flatMap(item -> item.materials().stream()).flatMap(item -> item.lots().stream())
                .filter(item -> !"VALID".equals(item.expirationStatus())).count();
    }
}
