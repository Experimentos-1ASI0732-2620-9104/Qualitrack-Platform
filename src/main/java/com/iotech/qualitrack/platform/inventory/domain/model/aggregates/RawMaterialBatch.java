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

/**
 * Represents a supplier lot of raw material received
 * by a laboratory.
 *
 * <p>Each lot maintains its supplier, identification number,
 * initial quantity, available quantity, reception date,
 * expiration date and quality status.</p>
 *
 * <p>Newly received lots begin in the QUARANTINED state.
 * Quality review operations determine whether a lot can
 * be released, observed or rejected.</p>
 *
 * <p>Only released lots with available stock and a valid
 * expiration period can be consumed.</p>
 *
 * <p>A supplier lot is distinct from a manufactured
 * product batch.</p>
 */
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


    /**
     * Creates a new supplier lot awaiting quality review.
     *
     * <p>The received quantity becomes both the initial
     * and available quantity. The lot starts in the
     * QUARANTINED state.</p>
     *
     * @param laboratoryId the laboratory identifier
     * @param rawMaterialId the associated material identifier
     * @param supplier the supplier name
     * @param batchNumber the supplier lot number
     * @param unit the measurement unit
     * @param amount the received quantity
     * @param receivedOn the reception date
     * @param expiresOn the expiration date
     * @return a new quarantined supplier lot
     */
    public static RawMaterialBatch receive(
            Long laboratoryId, Long rawMaterialId, String supplier,
            String batchNumber, String unit, BigDecimal amount,
            LocalDate receivedOn, LocalDate expiresOn) {

        return new RawMaterialBatch(
                null, laboratoryId, rawMaterialId, supplier, batchNumber,
                unit, amount, amount, receivedOn, expiresOn,
                RawMaterialBatchStatus.QUARANTINED);
    }


    /**
     * Determines whether a supplier lot can be consumed
     * on a specified date.
     *
     * <p>A lot is usable when it is released, contains
     * available stock and the reference date is within
     * its validity period.</p>
     *
     * <p>The expiration date itself is excluded
     * from the usable period.</p>
     *
     * @param onDate the reference business date
     * @return true when the lot is available for consumption
     */
    public boolean isUsableOn(LocalDate onDate) {
        Objects.requireNonNull(onDate, "Reference date is required");
        return status == RawMaterialBatchStatus.RELEASED && availableAmount.signum() > 0
                && !onDate.isBefore(receivedOn) && onDate.isBefore(expiresOn);
    }


    /**
     * Determines the expiration status of a supplier lot.
     *
     * <p>Returns EXPIRED when the reference date reaches
     * or exceeds the expiration date.</p>
     *
     * <p>Returns NEAR_EXPIRY when the lot expires within
     * the specified monitoring period. Otherwise,
     * the lot is considered VALID.</p>
     *
     * @param onDate the reference business date
     * @param nearExpiryDays the number of days used to identify upcoming expiration
     * @return the expiration status of the supplier lot
     * @throws IllegalArgumentException when nearExpiryDays is negative
     */
    public ExpirationStatus expirationStatus(LocalDate onDate, int nearExpiryDays) {
        Objects.requireNonNull(onDate, "Reference date is required");
        if (nearExpiryDays < 0) throw new IllegalArgumentException("Near expiry days cannot be negative");
        if (!onDate.isBefore(expiresOn)) return ExpirationStatus.EXPIRED;
        return expiresOn.isAfter(onDate.plusDays(nearExpiryDays)) ? ExpirationStatus.VALID : ExpirationStatus.NEAR_EXPIRY;
    }


    /**
     * Releases a supplier lot after quality review.
     *
     * <p>Only lots currently in the QUARANTINED state
     * may transition to RELEASED.</p>
     *
     * @throws IllegalStateException when the lot is not quarantined
     */
    public void release() {
        if (status != RawMaterialBatchStatus.QUARANTINED) {
            throw new IllegalStateException("Only quarantined receipts can be released after review");
        }
        status = RawMaterialBatchStatus.RELEASED;
    }


    /**
     * Marks a released supplier lot as observed.
     *
     * <p>Only lots in the RELEASED state may transition
     * to OBSERVED.</p>
     *
     * @throws IllegalStateException when the lot is not released
     */
    public void observe() {
        if (status != RawMaterialBatchStatus.RELEASED) {
            throw new IllegalStateException("Only released receipts can be marked observed");
        }
        status = RawMaterialBatchStatus.OBSERVED;
    }


    /**
     * Rejects a supplier lot during quality review.
     *
     * <p>A lot cannot be rejected again when it is
     * already in the REJECTED state.</p>
     *
     * @throws IllegalStateException when the lot is already rejected
     */
    public void reject() {
        if (status == RawMaterialBatchStatus.REJECTED) {
            throw new IllegalStateException("Receipt is already rejected");
        }
        status = RawMaterialBatchStatus.REJECTED;
    }

    /**
     * Consumes a quantity from the available stock
     * of a supplier lot.
     *
     * <p>The requested unit must match the lot unit.
     * The quantity must be valid and must not exceed
     * the available stock.</p>
     *
     * <p>Consumption is permitted only when the lot
     * is usable on the reference date.</p>
     *
     * @param amount the quantity to consume
     * @param requestedUnit the requested measurement unit
     * @param onDate the reference business date
     * @throws IllegalArgumentException when the quantity or unit is invalid
     * @throws IllegalStateException when the lot cannot be consumed or lacks stock
     */
    public void consume(BigDecimal amount, String requestedUnit, LocalDate onDate) {
        StockUnit.requireSame(unit, requestedUnit);
        StockUnit.validateQuantity(amount, unit, false);
        if (!isUsableOn(onDate)) throw new IllegalStateException("Receipt is not available for consumption");
        if (amount.compareTo(availableAmount) > 0) throw new IllegalStateException("Insufficient stock in receipt");
        availableAmount = availableAmount.subtract(amount);
    }

    /**
     * Retrieves the current container assignment of the lot.
     *
     * @return the container assignment, or an empty result
     *         when no container has been assigned
     */
    public Optional<ContainerAssignment> container() {
        return Optional.ofNullable(containerAssignment);
    }

    /**
     * Assigns the supplier lot to a monitored storage container.
     *
     * <p>An existing container assignment is replaced
     * when a different container is selected.</p>
     *
     * <p>Assigning the same container again does not
     * modify the current assignment.</p>
     *
     * @param assignment the monitored container assignment
     * @return true when the assignment changes,
     *         false when the lot is already in that container
     */
    public boolean storeIn(ContainerAssignment assignment) {
        Objects.requireNonNull(assignment, "Container assignment is required");
        if (containerAssignment != null && containerAssignment.isIn(assignment.containerMonitorId())) return false;
        containerAssignment = assignment;
        return true;
    }
}
