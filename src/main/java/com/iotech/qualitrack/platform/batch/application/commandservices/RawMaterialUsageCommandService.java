package com.iotech.qualitrack.platform.batch.application.commandservices;

import com.iotech.qualitrack.platform.batch.domain.model.commands.RegisterRawMaterialUsageCommand;
import com.iotech.qualitrack.platform.batch.domain.model.entities.RawMaterialUsage;
import com.iotech.qualitrack.platform.shared.application.result.ApplicationError;
import com.iotech.qualitrack.platform.shared.application.result.Result;

/**
 * Application service contract for the raw material consumed by product batches.
 */
public interface RawMaterialUsageCommandService {

    /**
     * Consumes a raw material lot for an open product batch through Inventory Management and records
     * the usage (US75, TS65).
     *
     * @param command the batch, lot, amount and idempotency key
     * @return the recorded usage, NOT_FOUND when the batch is not a batch of the product, CONFLICT when the
     * batch is closed; Inventory failures (unknown lot, unusable lot, insufficient stock) are raised as
     * {@link com.iotech.qualitrack.platform.shared.application.result.ApplicationException}
     */
    Result<RawMaterialUsage, ApplicationError> handle(RegisterRawMaterialUsageCommand command);
}
