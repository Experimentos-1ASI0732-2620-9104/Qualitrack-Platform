package com.iotech.qualitrack.platform.inventory.domain.model.commands;

import com.iotech.qualitrack.platform.inventory.domain.model.valueobjects.RawMaterialBatchStatus;

/**
 * Command to register a quality review decision for a supplier
 * raw material batch.
 *
 * <p>Identifies the batch to be reviewed and provides the requested
 * quality status together with the reason for the decision.</p>
 *
 * @param laboratoryId identifier of the laboratory owning the batch
 * @param environmentId identifier of the environment containing the material
 * @param materialId identifier of the raw material associated with the batch
 * @param receiptId identifier of the supplier batch being reviewed
 * @param status quality status requested for the batch
 * @param reason explanation or justification for the review decision
 */
public record ReviewRawMaterialBatchCommand(
        Long laboratoryId,
        Long environmentId,
        Long materialId,
        Long receiptId,
        RawMaterialBatchStatus status,
        String reason
) { }
