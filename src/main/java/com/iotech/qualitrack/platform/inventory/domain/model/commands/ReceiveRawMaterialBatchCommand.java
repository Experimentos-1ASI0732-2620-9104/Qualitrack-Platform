package com.iotech.qualitrack.platform.inventory.domain.model.commands;

import java.math.BigDecimal;
import java.time.LocalDate;


/**
 * Command to register the reception of a supplier raw material batch
 * within a laboratory environment.
 *
 * <p>Contains the information required to identify the supplier batch,
 * register its received quantity and establish its reception and
 * expiration dates.</p>
 *
 * @param laboratoryId identifier of the laboratory receiving the batch
 * @param environmentId identifier of the environment where the material is registered
 * @param materialId identifier of the raw material associated with the batch
 * @param supplier name of the supplier providing the raw material
 * @param batchNumber identification number assigned to the supplier batch
 * @param unit measurement unit used for the received quantity
 * @param amount quantity of raw material received
 * @param receivedOn date on which the batch was received
 * @param expiresOn expiration date assigned to the supplier batch
 */
public record ReceiveRawMaterialBatchCommand(
        Long laboratoryId,
        Long environmentId,
        Long materialId,
        String supplier,
        String batchNumber,
        String unit,
        BigDecimal amount,
        LocalDate receivedOn,
        LocalDate expiresOn
) { }
