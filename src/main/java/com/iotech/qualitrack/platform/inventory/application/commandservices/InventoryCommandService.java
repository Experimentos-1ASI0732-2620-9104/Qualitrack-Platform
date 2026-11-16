package com.iotech.qualitrack.platform.inventory.application.commandservices;

import com.iotech.qualitrack.platform.inventory.domain.model.valueobjects.RawMaterialBatchReview;import com.iotech.qualitrack.platform.inventory.domain.model.commands.*;
import com.iotech.qualitrack.platform.inventory.domain.model.aggregates.*;
import com.iotech.qualitrack.platform.inventory.domain.model.valueobjects.RawMaterialBatchContainer;
import com.iotech.qualitrack.platform.inventory.domain.model.valueobjects.ReceiptConsumption;
import com.iotech.qualitrack.platform.shared.application.result.*;
/**
 * Defines the application command operations of the Inventory bounded context.
 *
 * <p>This service coordinates operations related to raw material
 * registration, supplier lot reception, quality review, stock
 * consumption and container assignment.</p>
 *
 * <p>Implementations enforce application-level validations and
 * coordinate the corresponding domain and persistence operations.</p>
 */
public interface InventoryCommandService {

    /**
     * Creates or updates a raw material within a laboratory environment.
     *
     * <p>Material codes must be unique within the laboratory.
     * When updating an existing material, its unit of measurement
     * must remain consistent.</p>
     *
     * @param command the raw material data and its laboratory context
     * @return the result containing the saved raw material
     */
    Result<RawMaterial, ApplicationError> handle(SaveRawMaterialCommand command);

    /**
     * Registers a new supplier lot associated with a raw material.
     *
     * <p>A newly received lot starts in the QUARANTINED state
     * and requires quality review before becoming available
     * for consumption.</p>
     *
     * @param command the supplier, lot identifier, quantity,
     *                unit and reception dates
     * @return the result containing the registered supplier lot
     */
    Result<RawMaterialBatch, ApplicationError> handle(ReceiveRawMaterialBatchCommand command);

    /**
     * Reviews the quality status of a raw material supplier lot.
     *
     * <p>Supported decisions include releasing, observing
     * or rejecting a lot, subject to its current state.
     * The operation also records the review movement.</p>
     *
     * @param command the target status and justification
     *                for the quality review
     * @return the result containing the review details
     */
    Result<RawMaterialBatchReview, ApplicationError> handle(ReviewRawMaterialBatchCommand command);

    /**
     * Consumes a quantity from a raw material lot for a product batch.
     *
     * <p>The lot must be usable and have sufficient available stock.
     * The operation is idempotent: repeating the same operation
     * identifier with identical data does not consume stock twice.</p>
     *
     * @param command the supplier lot, product batch, quantity,
     *                unit and idempotency identifier
     * @return the result containing the consumption details
     *         and stock balances
     */
    Result<ReceiptConsumption, ApplicationError> handle(ConsumeRawMaterialBatchCommand command);


    /**
     * Assigns a raw material lot to a monitored storage container.
     *
     * <p>The container must belong to the corresponding environment
     * and be operational. The environment must be designated
     * for raw material storage.</p>
     *
     * <p>Assigning a lot to its current container does not
     * generate an additional storage movement.</p>
     *
     * @param command the supplier lot and target container information
     * @return the result containing the container assignment details
     */
    Result<RawMaterialBatchContainer, ApplicationError> handle(AssignRawMaterialBatchContainerCommand command);
}
