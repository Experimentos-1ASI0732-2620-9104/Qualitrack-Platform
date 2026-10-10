package com.iotech.qualitrack.platform.inventory.interfaces.rest.transform;

import com.iotech.qualitrack.platform.inventory.domain.model.commands.ReviewRawMaterialBatchCommand;
import com.iotech.qualitrack.platform.inventory.interfaces.rest.resources.ReviewRawMaterialBatchResource;

public final class ReviewRawMaterialBatchCommandFromResourceAssembler {
    private ReviewRawMaterialBatchCommandFromResourceAssembler() { }

    public static ReviewRawMaterialBatchCommand toCommandFromResource(Long lab, Long environment, Long material, Long receipt,
            ReviewRawMaterialBatchResource resource) {
        return new ReviewRawMaterialBatchCommand(lab, environment, material, receipt, resource.status(), resource.reason());
    }
}
