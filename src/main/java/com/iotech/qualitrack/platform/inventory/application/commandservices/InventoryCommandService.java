package com.iotech.qualitrack.platform.inventory.application.commandservices;

import com.iotech.qualitrack.platform.inventory.domain.model.valueobjects.RawMaterialBatchReview;import com.iotech.qualitrack.platform.inventory.domain.model.commands.*;
import com.iotech.qualitrack.platform.inventory.domain.model.aggregates.*;
import com.iotech.qualitrack.platform.inventory.domain.model.valueobjects.RawMaterialBatchContainer;
import com.iotech.qualitrack.platform.inventory.domain.model.valueobjects.ReceiptConsumption;
import com.iotech.qualitrack.platform.shared.application.result.*;

public interface InventoryCommandService {
    Result<RawMaterial, ApplicationError> handle(SaveRawMaterialCommand command);
    Result<RawMaterialBatch, ApplicationError> handle(ReceiveRawMaterialBatchCommand command);
    Result<RawMaterialBatchReview, ApplicationError> handle(ReviewRawMaterialBatchCommand command);
    Result<ReceiptConsumption, ApplicationError> handle(ConsumeRawMaterialBatchCommand command);
    /** Stores a raw material lot in a monitored container of its environment (US43, TS29). */
    Result<RawMaterialBatchContainer, ApplicationError> handle(AssignRawMaterialBatchContainerCommand command);
}
