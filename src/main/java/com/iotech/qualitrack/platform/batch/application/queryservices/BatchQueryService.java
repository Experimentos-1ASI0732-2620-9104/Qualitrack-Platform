package com.iotech.qualitrack.platform.batch.application.queryservices;

import com.iotech.qualitrack.platform.batch.domain.model.queries.GetBatchContainerQuery;
import com.iotech.qualitrack.platform.batch.domain.model.valueobjects.BatchContainer;
import com.iotech.qualitrack.platform.batch.domain.model.aggregates.Batch;
import com.iotech.qualitrack.platform.batch.domain.model.queries.GetBatchByIdQuery;
import com.iotech.qualitrack.platform.batch.domain.model.queries.GetBatchesByLabIdQuery;
import com.iotech.qualitrack.platform.batch.domain.model.queries.GetBatchesByProductQuery;
import com.iotech.qualitrack.platform.batch.domain.model.queries.GetProductBatchQuery;

import java.util.List;
import java.util.Optional;

/**
 * Read side of product batches.
 */
public interface BatchQueryService {
    Optional<Batch> handle(GetBatchByIdQuery query);

    List<Batch> handle(GetBatchesByLabIdQuery query);

    /**
     * Batches of a product, newest start date first.
     *
     * @return the batches, or empty when the product is not registered in the environment
     */
    Optional<List<Batch>> handle(GetBatchesByProductQuery query);

    Optional<Batch> handle(GetProductBatchQuery query);

    /**
     * Container where the batch is stored (US79, TS69).
     *
     * @return the container, or empty when the batch has no container
     * @throws com.iotech.qualitrack.platform.shared.application.result.ApplicationException not found when the batch
     * is not registered for the product
     */
    Optional<BatchContainer> handle(GetBatchContainerQuery query);
}
