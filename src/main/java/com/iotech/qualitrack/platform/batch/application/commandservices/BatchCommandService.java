package com.iotech.qualitrack.platform.batch.application.commandservices;

import com.iotech.qualitrack.platform.batch.domain.model.commands.AssignBatchContainerCommand;
import com.iotech.qualitrack.platform.batch.domain.model.valueobjects.BatchContainer;
import com.iotech.qualitrack.platform.batch.domain.model.aggregates.Batch;
import com.iotech.qualitrack.platform.batch.domain.model.commands.CreateBatchCommand;
import com.iotech.qualitrack.platform.batch.domain.model.commands.RejectBatchCommand;
import com.iotech.qualitrack.platform.batch.domain.model.commands.ReleaseBatchCommand;
import com.iotech.qualitrack.platform.batch.domain.model.valueobjects.BatchRejection;
import com.iotech.qualitrack.platform.batch.domain.model.valueobjects.BatchRelease;
import com.iotech.qualitrack.platform.shared.application.result.ApplicationError;
import com.iotech.qualitrack.platform.shared.application.result.Result;

/**
 * Application service for the lifecycle of product batches.
 */
public interface BatchCommandService {
    /**
     * Registers a pending batch of a product (US73, TS63).
     *
     * @return the batch, NOT_FOUND when the product is not in the environment, CONFLICT when the
     * batch number is already used in the laboratory
     */
    Result<Batch, ApplicationError> handle(CreateBatchCommand command);

    /**
     * Releases a batch and stores the signature of the current user (US81, TS71).
     *
     * @return the release, NOT_FOUND when the batch is not a batch of the product, CONFLICT when the
     * batch is already released or rejected
     */
    Result<BatchRelease, ApplicationError> handle(ReleaseBatchCommand command);

    /**
     * Rejects a batch and keeps the reason (US82, TS72).
     *
     * @return the rejection, NOT_FOUND when the batch is not a batch of the product, CONFLICT when the
     * batch is already released or rejected
     */
    Result<BatchRejection, ApplicationError> handle(RejectBatchCommand command);

    /**
     * Stores the batch in a monitored container of a product storage environment (US78, TS68).
     *
     * @return the container of the batch, not found when the batch is not registered for the product, a validation
     * error without container, or a conflict when the container cannot receive the batch
     */
    Result<BatchContainer, ApplicationError> handle(AssignBatchContainerCommand command);
}
