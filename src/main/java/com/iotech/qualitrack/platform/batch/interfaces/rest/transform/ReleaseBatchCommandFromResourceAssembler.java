package com.iotech.qualitrack.platform.batch.interfaces.rest.transform;

import com.iotech.qualitrack.platform.batch.domain.model.commands.ReleaseBatchCommand;
import com.iotech.qualitrack.platform.batch.domain.model.valueobjects.BatchRelease;
import com.iotech.qualitrack.platform.batch.interfaces.rest.resources.BatchReleaseResource;
import com.iotech.qualitrack.platform.batch.interfaces.rest.resources.ReleaseBatchResource;

/**
 * Maps batch releases between REST and the domain.
 */
public final class ReleaseBatchCommandFromResourceAssembler {

    private ReleaseBatchCommandFromResourceAssembler() {
    }

    public static ReleaseBatchCommand toCommandFromResource(Long laboratoryId, Long environmentId, Long productId,
                                                            Long batchId, ReleaseBatchResource resource) {
        return new ReleaseBatchCommand(laboratoryId, environmentId, productId, batchId, resource.releaseDate(), resource.notes());
    }

    public static BatchReleaseResource toResourceFromRelease(BatchRelease release) {
        var batch = release.batch();
        var signature = release.signature();
        return new BatchReleaseResource(batch.getId(), batch.getBatchNumber(), batch.getStatus().name(), batch.getEndDate(),
                batch.getNotes(), signature.getSignedByUserId(), signature.getSignatureHash(), signature.getSignedAt());
    }
}
