package com.iotech.qualitrack.platform.inventory.interfaces.rest.transform;

import com.iotech.qualitrack.platform.inventory.domain.model.commands.ReceiveRawMaterialBatchCommand;
import com.iotech.qualitrack.platform.inventory.interfaces.rest.resources.ReceiveRawMaterialBatchResource;

public final class ReceiveRawMaterialBatchCommandFromResourceAssembler {
    private ReceiveRawMaterialBatchCommandFromResourceAssembler() { }

    public static ReceiveRawMaterialBatchCommand toCommandFromResource(Long lab, Long environment, Long material,
            ReceiveRawMaterialBatchResource resource) {
        return new ReceiveRawMaterialBatchCommand(lab, environment, material, resource.supplier(), resource.batchNumber(),
            resource.unit(), resource.amount(), resource.receivedOn(), resource.expiresOn());
    }
}
