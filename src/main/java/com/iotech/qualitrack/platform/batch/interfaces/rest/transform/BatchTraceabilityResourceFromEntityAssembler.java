package com.iotech.qualitrack.platform.batch.interfaces.rest.transform;

import com.iotech.qualitrack.platform.batch.domain.model.valueobjects.BatchTraceability;
import com.iotech.qualitrack.platform.batch.interfaces.rest.resources.BatchTraceabilityResource;
import com.iotech.qualitrack.platform.batch.interfaces.rest.resources.BatchTraceabilityResource.*;

/**
 * Maps the traceability of a batch to its REST representation.
 */
public final class BatchTraceabilityResourceFromEntityAssembler {

    private BatchTraceabilityResourceFromEntityAssembler() {
    }

    public static BatchTraceabilityResource toResourceFromEntity(BatchTraceability traceability) {
        var product = traceability.product();
        return new BatchTraceabilityResource(
                BatchResourceFromEntityAssembler.toResourceFromEntity(traceability.batch()),
                new ProductSummaryResource(product.getId(), product.getCode(), product.getName()),
                traceability.rawMaterials().stream().map(traced -> {
                    var usage = traced.usage();
                    return new TracedRawMaterialUsageResource(usage.getId(), usage.getRawMaterialId(), usage.getRawMaterialName(),
                            traced.rawMaterialEnvironmentId(), usage.getInventoryReceiptId(), usage.getQuantityUsed(), usage.getUnit(),
                            usage.getUsageDate(), usage.getStockBefore(), usage.getStockAfter());
                }).toList(),
                traceability.equipment().stream().map(BatchParticipationResourceFromEntityAssembler::toResourceFromEntity).toList(),
                traceability.staff().stream().map(BatchParticipationResourceFromEntityAssembler::toResourceFromEntity).toList(),
                traceability.release().map(signature -> new ReleaseEvidenceResource(signature.getSignedByUserId(),
                        signature.getSignatureHash(), signature.getSignedAt())).orElse(null),
                traceability.rejection().map(record -> new RejectionEvidenceResource(record.getRejectionDate(),
                        record.getReason())).orElse(null),
                traceability.container().map(BatchContainerResourceFromEntityAssembler::toResourceFromEntity).orElse(null));
    }
}
