package com.iotech.qualitrack.platform.batch.application.queryservices;

import com.iotech.qualitrack.platform.batch.domain.model.entities.RawMaterialUsage;
import com.iotech.qualitrack.platform.batch.domain.model.queries.GetRawMaterialUsageByBatchIdQuery;

import java.util.List;

/**
 * Application service contract for raw material usage read queries.
 */
public interface RawMaterialUsageQueryService {

    List<RawMaterialUsage> handle(com.iotech.qualitrack.platform.batch.domain.model.queries.GetRawMaterialHistoryQuery query);

    /**
     * Handles retrieval of the product batch usages recorded when lots of an Inventory raw material were consumed.
     *
     * @param query Inventory raw material query
     * @return usages newest first
     */
    List<RawMaterialUsage> handle(com.iotech.qualitrack.platform.batch.domain.model.queries.GetRawMaterialUsagesByInventoryMaterialQuery query);

    /**
     * Handles retrieval of all raw material usage records associated with a specific batch.
     *
     * @param query batch-id query
     * @return list of raw material usage records for the given batch
     * @see GetRawMaterialUsageByBatchIdQuery
     */
    List<RawMaterialUsage> handle(GetRawMaterialUsageByBatchIdQuery query);
}
