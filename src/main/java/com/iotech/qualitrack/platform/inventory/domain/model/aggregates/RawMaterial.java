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
 * Represents the catalog identity of a raw material
 * registered within a laboratory environment.
 *
 * <p>A raw material maintains its identification,
 * measurement unit and minimum stock threshold.</p>
 *
 * <p>Supplier information, expiration dates and
 * inventory quantities belong to individual supplier lots
 * rather than the material itself.</p>
 *
 * <p>Stock availability is calculated from registered lots,
 * avoiding duplicated stock counters.</p>
 *
 * <p>The environment identifier may be null only for
 * materials registered before environments were introduced.</p>
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



    /**
     * Creates or reconstructs a raw material aggregate.
     *
     * <p>Validates the identifiers, material code, name,
     * measurement unit and minimum stock quantity.</p>
     *
     * <p>Material codes and names are normalized by removing
     * surrounding whitespace. The measurement unit is
     * normalized according to the supported stock units.</p>
     *
     * @param id the material identifier, or null for a new material
     * @param laboratoryId the owning laboratory identifier
     * @param environmentId the assigned environment identifier, when available
     * @param code the material identification code
     * @param name the material name
     * @param unit the measurement unit
     * @param minimumStock the minimum stock threshold
     * @throws IllegalArgumentException when the supplied material data is invalid
     */
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


    /**
     * Calculates the usable stock of a raw material
     * from its registered supplier lots.
     *
     * @param receipts the supplier lots associated with the material
     * @param onDate the reference date for stock availability
     * @return the total usable quantity of the material
     * @throws IllegalArgumentException when a lot belongs to another material or violates stock rules
     * @throws IllegalStateException when the material is not registered
     * @throws NullPointerException when required arguments are null
     */
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

    /**
     * Determines whether this raw material belongs
     * to the specified laboratory environment.
     *
     * @param environmentId the environment identifier to verify
     * @return true when the material is assigned to the environment, false otherwise
     */
    public boolean belongsToEnvironment(Long environmentId) {
        return this.environmentId != null && this.environmentId.equals(environmentId);
    }

}
