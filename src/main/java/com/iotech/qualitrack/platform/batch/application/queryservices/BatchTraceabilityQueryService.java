package com.iotech.qualitrack.platform.batch.application.queryservices;

import com.iotech.qualitrack.platform.batch.domain.model.queries.GetBatchTraceabilityQuery;
import com.iotech.qualitrack.platform.batch.domain.model.valueobjects.BatchTraceability;

import java.util.Optional;

/**
 * Reconstructs which resources took part in a product batch.
 */
public interface BatchTraceabilityQueryService {
    /**
     * @return the traceability, or empty when the batch is not a batch of the product in the environment
     */
    Optional<BatchTraceability> handle(GetBatchTraceabilityQuery query);
}
