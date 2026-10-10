package com.iotech.qualitrack.platform.batch.interfaces.rest.transform;

import com.iotech.qualitrack.platform.batch.domain.model.commands.RejectBatchCommand;
import com.iotech.qualitrack.platform.batch.domain.model.valueobjects.BatchRejection;
import com.iotech.qualitrack.platform.batch.interfaces.rest.resources.BatchRejectionResource;
import com.iotech.qualitrack.platform.batch.interfaces.rest.resources.RejectBatchResource;

/**
 * Maps batch rejections between REST and the domain.
 */
public final class RejectBatchCommandFromResourceAssembler {

    private RejectBatchCommandFromResourceAssembler() {
    }

    public static RejectBatchCommand toCommandFromResource(Long laboratoryId, Long environmentId, Long productId,
                                                           Long batchId, RejectBatchResource resource) {
        return new RejectBatchCommand(laboratoryId, environmentId, productId, batchId, resource.rejectionDate(), resource.reason());
    }

    public static BatchRejectionResource toResourceFromRejection(BatchRejection rejection) {
        var batch = rejection.batch();
        var record = rejection.record();
        return new BatchRejectionResource(record.getId(), batch.getId(), batch.getBatchNumber(), batch.getStatus().name(),
                record.getRejectionDate(), record.getReason());
    }
}
