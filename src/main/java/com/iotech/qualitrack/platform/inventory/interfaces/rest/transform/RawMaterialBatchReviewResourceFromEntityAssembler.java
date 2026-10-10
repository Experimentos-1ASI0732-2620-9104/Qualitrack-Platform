package com.iotech.qualitrack.platform.inventory.interfaces.rest.transform;

import com.iotech.qualitrack.platform.inventory.domain.model.valueobjects.RawMaterialBatchReview;
import com.iotech.qualitrack.platform.inventory.interfaces.rest.resources.RawMaterialBatchReviewResource;

public final class RawMaterialBatchReviewResourceFromEntityAssembler {
    private RawMaterialBatchReviewResourceFromEntityAssembler() { }

    public static RawMaterialBatchReviewResource toResourceFromEntity(RawMaterialBatchReview review) {
        return new RawMaterialBatchReviewResource(review.rawMaterialBatchId(), review.rawMaterialId(), review.previousStatus(),
            review.status(), review.reason(), review.reviewedBy(), review.reviewedAt());
    }
}
