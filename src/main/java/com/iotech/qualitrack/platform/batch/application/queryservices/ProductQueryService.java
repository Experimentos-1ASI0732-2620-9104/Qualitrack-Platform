package com.iotech.qualitrack.platform.batch.application.queryservices;

import com.iotech.qualitrack.platform.batch.domain.model.aggregates.PharmaceuticalProduct;
import com.iotech.qualitrack.platform.batch.domain.model.queries.GetProductByIdQuery;
import com.iotech.qualitrack.platform.batch.domain.model.queries.GetProductsByEnvironmentQuery;

import java.util.List;
import java.util.Optional;

/**
 * Read side of the pharmaceutical product catalog.
 */
public interface ProductQueryService {
    List<PharmaceuticalProduct> handle(GetProductsByEnvironmentQuery query);

    Optional<PharmaceuticalProduct> handle(GetProductByIdQuery query);
}
