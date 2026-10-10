package com.iotech.qualitrack.platform.inventory.domain.model.aggregates;

import com.iotech.qualitrack.platform.inventory.domain.model.valueobjects.ContainerAssignment;
import com.iotech.qualitrack.platform.inventory.domain.model.valueobjects.ExpirationStatus;
import com.iotech.qualitrack.platform.inventory.domain.model.valueobjects.RawMaterialBatchStatus;
import com.iotech.qualitrack.platform.shared.domain.model.aggregates.AbstractDomainAggregateRoot;
import com.iotech.qualitrack.platform.shared.domain.model.valueobjects.StockUnit;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

/** A supplier receipt, not a manufactured product batch. */
@Getter
public class RawMaterialBatch extends AbstractDomainAggregateRoot<RawMaterialBatch> {
    private final Long id;
    private final Long laboratoryId;
    private final Long rawMaterialId;
    private final String supplier;
    private final String batchNumber;
    private final String unit;
    private final BigDecimal initialAmount;
    private BigDecimal availableAmount;
    private final LocalDate receivedOn;
    private final LocalDate expiresOn;
    private RawMaterialBatchStatus status;
    /** Monitored container where the lot is stored; null while it has no container (US43, US44). */
    private ContainerAssignment containerAssignment;

    /** Reconstructs a receipt; quality review authorization is enforced at the API boundary. */
    public RawMaterialBatch(Long id, Long laboratoryId, Long rawMaterialId, String supplier,
                            String batchNumber, String unit, BigDecimal initialAmount,
                            BigDecimal availableAmount, LocalDate receivedOn, LocalDate expiresOn,
                            RawMaterialBatchStatus status) {
        this(id, laboratoryId, rawMaterialId, supplier, batchNumber, unit, initialAmount, availableAmount, receivedOn,
                expiresOn, status, null);
    }

    /** Reconstructs a receipt together with the container where it is stored. */
    public RawMaterialBatch(Long id, Long laboratoryId, Long rawMaterialId, String supplier,
                            String batchNumber, String unit, BigDecimal initialAmount,
                            BigDecimal availableAmount, LocalDate receivedOn, LocalDate expiresOn,
                            RawMaterialBatchStatus status, ContainerAssignment containerAssignment) {
        if (id != null && id <= 0) throw new IllegalArgumentException("Invalid receipt ID");
        if (laboratoryId == null || laboratoryId <= 0) throw new IllegalArgumentException("Invalid laboratory ID");
        if (rawMaterialId == null || rawMaterialId <= 0) throw new IllegalArgumentException("Invalid material ID");
        if (supplier == null || supplier.isBlank()) throw new IllegalArgumentException("Supplier is required");
        if (batchNumber == null || batchNumber.isBlank()) throw new IllegalArgumentException("Receipt batch number is required");
        this.unit = StockUnit.normalize(unit);
        StockUnit.validateQuantity(initialAmount, this.unit, false);
        StockUnit.validateQuantity(availableAmount, this.unit, true);
        if (availableAmount.compareTo(initialAmount) > 0) {
            throw new IllegalArgumentException("Available amount cannot exceed received amount");
        }
        this.receivedOn = Objects.requireNonNull(receivedOn, "Receipt date is required");
        this.expiresOn = Objects.requireNonNull(expiresOn, "Expiration date is required");
        if (expiresOn.isBefore(receivedOn)) throw new IllegalArgumentException("Expiration precedes receipt date");
        this.id = id;
        this.laboratoryId = laboratoryId;
        this.rawMaterialId = rawMaterialId;
        this.supplier = supplier.trim();
        this.batchNumber = batchNumber.trim();
        this.initialAmount = initialAmount;
        this.availableAmount = availableAmount;
        this.status = Objects.requireNonNull(status, "Receipt status is required");
        this.containerAssignment = containerAssignment;
    }

    /** A newly received lot requires review before consumption. */
    public static RawMaterialBatch receive(Long laboratoryId, Long rawMaterialId, String supplier,
                                            String batchNumber, String unit, BigDecimal amount,
                                            LocalDate receivedOn, LocalDate expiresOn) {
        return new RawMaterialBatch(null, laboratoryId, rawMaterialId, supplier, batchNumber,
                unit, amount, amount, receivedOn, expiresOn, RawMaterialBatchStatus.QUARANTINED);
    }

    /** Date-only policy: expiration day is excluded; caller supplies the laboratory's business date. */
    public boolean isUsableOn(LocalDate onDate) {
        Objects.requireNonNull(onDate, "Reference date is required");
        return status == RawMaterialBatchStatus.RELEASED && availableAmount.signum() > 0
                && !onDate.isBefore(receivedOn) && onDate.isBefore(expiresOn);
    }

    /**
     * Classifies the expiration of the lot on a business date.
     * A lot is near expiry when it expires within the next {@code nearExpiryDays} days and is not yet expired.
     */
    public ExpirationStatus expirationStatus(LocalDate onDate, int nearExpiryDays) {
        Objects.requireNonNull(onDate, "Reference date is required");
        if (nearExpiryDays < 0) throw new IllegalArgumentException("Near expiry days cannot be negative");
        if (!onDate.isBefore(expiresOn)) return ExpirationStatus.EXPIRED;
        return expiresOn.isAfter(onDate.plusDays(nearExpiryDays)) ? ExpirationStatus.VALID : ExpirationStatus.NEAR_EXPIRY;
    }

    public void release() {
        if (status != RawMaterialBatchStatus.QUARANTINED) {
            throw new IllegalStateException("Only quarantined receipts can be released after review");
        }
        status = RawMaterialBatchStatus.RELEASED;
    }

    public void observe() {
        if (status != RawMaterialBatchStatus.RELEASED) {
            throw new IllegalStateException("Only released receipts can be marked observed");
        }
        status = RawMaterialBatchStatus.OBSERVED;
    }

    public void reject() {
        if (status == RawMaterialBatchStatus.REJECTED) {
            throw new IllegalStateException("Receipt is already rejected");
        }
        status = RawMaterialBatchStatus.REJECTED;
    }

    /**
     * Stores the lot in a monitored container, replacing the previous one (US43).
     *
     * @param assignment the container, its environment and who stored the lot
     * @return false when the lot was already stored in that container
     */
    public boolean storeIn(ContainerAssignment assignment) {
        Objects.requireNonNull(assignment, "Container assignment is required");
        if (containerAssignment != null && containerAssignment.isIn(assignment.containerMonitorId())) return false;
        containerAssignment = assignment;
        return true;
    }

    /** @return the container where the lot is stored, if any (US44) */
    public Optional<ContainerAssignment> container() {
        return Optional.ofNullable(containerAssignment);
    }

    public void consume(BigDecimal amount, String requestedUnit, LocalDate onDate) {
        StockUnit.requireSame(unit, requestedUnit);
        StockUnit.validateQuantity(amount, unit, false);
        if (!isUsableOn(onDate)) throw new IllegalStateException("Receipt is not available for consumption");
        if (amount.compareTo(availableAmount) > 0) throw new IllegalStateException("Insufficient stock in receipt");
        availableAmount = availableAmount.subtract(amount);
    }
}
