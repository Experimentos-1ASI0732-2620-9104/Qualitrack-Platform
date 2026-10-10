package com.iotech.qualitrack.platform.batch.application.internal.queryservices;

import com.iotech.qualitrack.platform.batch.application.internal.outboundservices.acl.BatchExternalEquipmentService;
import com.iotech.qualitrack.platform.batch.domain.model.queries.GetBatchContainerQuery;
import com.iotech.qualitrack.platform.batch.domain.model.valueobjects.BatchContainer;
import com.iotech.qualitrack.platform.equipment.interfaces.acl.EquipmentContextFacade;
import com.iotech.qualitrack.platform.shared.application.result.ApplicationError;
import com.iotech.qualitrack.platform.shared.application.result.ApplicationException;
import com.iotech.qualitrack.platform.batch.application.queryservices.BatchQueryService;
import com.iotech.qualitrack.platform.batch.domain.model.aggregates.Batch;
import com.iotech.qualitrack.platform.batch.domain.model.queries.GetBatchByIdQuery;
import com.iotech.qualitrack.platform.batch.domain.model.queries.GetBatchesByLabIdQuery;
import com.iotech.qualitrack.platform.batch.domain.model.queries.GetBatchesByProductQuery;
import com.iotech.qualitrack.platform.batch.domain.model.queries.GetProductBatchQuery;
import com.iotech.qualitrack.platform.batch.domain.repositories.BatchRepository;
import com.iotech.qualitrack.platform.batch.domain.repositories.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Reads product batches, always checking that they belong to the requested product hierarchy.
 */
@Service
public class BatchQueryServiceImpl implements BatchQueryService {

    private final BatchRepository batchRepository;
    private final ProductRepository productRepository;
    private final BatchExternalEquipmentService equipment;

    public BatchQueryServiceImpl(BatchRepository batchRepository, ProductRepository productRepository,
                                 BatchExternalEquipmentService equipment) {
        this.batchRepository = batchRepository;
        this.productRepository = productRepository;
        this.equipment = equipment;
    }

    @Override
    public Optional<BatchContainer> handle(GetBatchContainerQuery query) {
        var batch = batchRepository.findById(query.batchId())
                .filter(value -> value.belongsTo(query.laboratoryId(), query.environmentId(), query.productId()))
                .orElseThrow(() -> new ApplicationException(ApplicationError.notFound("Batch", query.batchId())));
        return batch.container().map(container -> new BatchContainer(batch.getId(), container.containerMonitorId(),
                equipment.findContainerMonitor(query.laboratoryId(), container.containerMonitorId())
                        .map(EquipmentContextFacade.ContainerReference::name).orElse(null),
                container.environmentId(), container.assignedBy(), container.assignedAt()));
    }

    @Override
    public Optional<Batch> handle(GetBatchByIdQuery query) {
        return batchRepository.findById(query.batchId());
    }

    @Override
    public List<Batch> handle(GetBatchesByLabIdQuery query) {
        return batchRepository.findAllByLabId(query.labId());
    }

    @Override
    public Optional<List<Batch>> handle(GetBatchesByProductQuery query) {
        return productRepository.findById(query.productId())
                .filter(product -> product.belongsTo(query.laboratoryId(), query.environmentId()))
                .map(product -> batchRepository.findAllByProductId(product.getId()).stream()
                        .filter(batch -> batch.belongsTo(query.laboratoryId(), query.environmentId(), query.productId()))
                        .toList());
    }

    @Override
    public Optional<Batch> handle(GetProductBatchQuery query) {
        return batchRepository.findById(query.batchId())
                .filter(batch -> batch.belongsTo(query.laboratoryId(), query.environmentId(), query.productId()));
    }
}
