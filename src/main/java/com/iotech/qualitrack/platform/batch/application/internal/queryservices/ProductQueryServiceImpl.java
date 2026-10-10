package com.iotech.qualitrack.platform.batch.application.internal.queryservices;

import com.iotech.qualitrack.platform.batch.application.queryservices.ProductQueryService;
import com.iotech.qualitrack.platform.batch.domain.model.aggregates.PharmaceuticalProduct;
import com.iotech.qualitrack.platform.batch.domain.model.queries.GetProductByIdQuery;
import com.iotech.qualitrack.platform.batch.domain.model.queries.GetProductsByEnvironmentQuery;
import com.iotech.qualitrack.platform.batch.domain.repositories.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Reads pharmaceutical products scoped to an environment of a laboratory.
 */
@Service
public class ProductQueryServiceImpl implements ProductQueryService {

    private final ProductRepository productRepository;

    public ProductQueryServiceImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public List<PharmaceuticalProduct> handle(GetProductsByEnvironmentQuery query) {
        return productRepository.findAllByLaboratoryIdAndEnvironmentId(query.laboratoryId(), query.environmentId());
    }

    @Override
    public Optional<PharmaceuticalProduct> handle(GetProductByIdQuery query) {
        return productRepository.findById(query.productId())
                .filter(product -> product.belongsTo(query.laboratoryId(), query.environmentId()));
    }
}
