package com.iotech.qualitrack.platform.inventory.domain.model.aggregates;

import com.iotech.qualitrack.platform.shared.domain.model.aggregates.AbstractDomainAggregateRoot;
import com.iotech.qualitrack.platform.shared.domain.model.valueobjects.StockUnit;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/**
 * Catalog identity of a raw material kept in an environment of the laboratory.
 * Suppliers, expiration and quantities belong to individual receipts.
 * The environment is null only for materials registered before environments existed.
 */
@Getter
public class RawMaterial extends AbstractDomainAggregateRoot<RawMaterial> {
    private final Long id;
    private final Long laboratoryId;
    private final Long environmentId;
    private final String code;
    private final String name;
    private final String unit;
    private final BigDecimal minimumStock;

    public RawMaterial(Long id, Long laboratoryId, Long environmentId, String code, String name,
                       String unit, BigDecimal minimumStock) {
        if (id != null && id <= 0) throw new IllegalArgumentException("Invalid material ID");
        if (laboratoryId == null || laboratoryId <= 0) throw new IllegalArgumentException("Invalid laboratory ID");
        if (environmentId != null && environmentId <= 0) throw new IllegalArgumentException("Invalid environment ID");
        if (code == null || code.isBlank()) throw new IllegalArgumentException("Material code is required");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Material name is required");
        this.unit = StockUnit.normalize(unit);
        StockUnit.validateQuantity(minimumStock, this.unit, true);
        this.id = id;
        this.laboratoryId = laboratoryId;
        this.environmentId = environmentId;
        this.code = code.trim();
        this.name = name.trim();
        this.minimumStock = minimumStock;
    }

    /** True when the material is kept in the given environment. */
    public boolean belongsToEnvironment(Long environmentId) {
        return this.environmentId != null && this.environmentId.equals(environmentId);
    }

    /** Calculates usable stock from distinct receipts; never stores a second stock counter. */
    public BigDecimal usableStock(List<RawMaterialBatch> receipts, LocalDate onDate) {
        Objects.requireNonNull(receipts, "Receipts are required");
        Objects.requireNonNull(onDate, "Reference date is required");
        if (id == null) throw new IllegalStateException("Material must be registered before calculating stock");
        var seen = new HashSet<Long>();
        var total = BigDecimal.ZERO;
        for (var receipt : receipts) {
            Objects.requireNonNull(receipt, "Receipt is required");
            if (!id.equals(receipt.getRawMaterialId()) || !laboratoryId.equals(receipt.getLaboratoryId())) {
                throw new IllegalArgumentException("Receipt does not belong to this material and laboratory");
            }
            StockUnit.requireSame(unit, receipt.getUnit());
            if (receipt.getId() == null || !seen.add(receipt.getId())) {
                throw new IllegalArgumentException("Stock requires distinct registered receipts");
            }
            if (receipt.isUsableOn(onDate)) total = total.add(receipt.getAvailableAmount());
        }
        return total;
    }
}
