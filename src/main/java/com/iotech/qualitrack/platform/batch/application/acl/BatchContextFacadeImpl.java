package com.iotech.qualitrack.platform.batch.application.acl;

import com.iotech.qualitrack.platform.batch.domain.repositories.BatchRepository;
import org.springframework.transaction.annotation.Transactional;

import com.iotech.qualitrack.platform.batch.application.queryservices.BatchQueryService;
import com.iotech.qualitrack.platform.batch.application.queryservices.BatchTraceabilityQueryService;
import com.iotech.qualitrack.platform.batch.application.queryservices.RawMaterialUsageQueryService;
import com.iotech.qualitrack.platform.batch.domain.model.aggregates.Batch;
import com.iotech.qualitrack.platform.batch.domain.model.entities.RawMaterialUsage;
import com.iotech.qualitrack.platform.batch.domain.model.queries.GetBatchTraceabilityQuery;
import com.iotech.qualitrack.platform.batch.domain.model.queries.GetBatchByIdQuery;
import com.iotech.qualitrack.platform.batch.domain.model.queries.GetBatchesByLabIdQuery;
import com.iotech.qualitrack.platform.batch.domain.model.queries.GetRawMaterialUsageByBatchIdQuery;
import com.iotech.qualitrack.platform.batch.domain.model.valueobjects.BatchStatus;
import com.iotech.qualitrack.platform.batch.interfaces.acl.BatchContextFacade;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Application-layer implementation of the Batch ACL facade.
 */
@Service
public class BatchContextFacadeImpl implements BatchContextFacade {

    private final BatchQueryService batchQueryService;
    private final BatchRepository repository;
    private final BatchTraceabilityQueryService traceabilityQueryService;
    private final RawMaterialUsageQueryService usageQueryService;

    public BatchContextFacadeImpl(BatchQueryService batchQueryService,
            BatchRepository repository, BatchTraceabilityQueryService traceabilityQueryService,
            RawMaterialUsageQueryService usageQueryService) {
        this.batchQueryService = batchQueryService;
        this.repository = repository;
        this.traceabilityQueryService = traceabilityQueryService;
        this.usageQueryService = usageQueryService;
    }

    @Override
    public boolean existsBatchById(Long batchId) {
        var query = new GetBatchByIdQuery(batchId);

        var result = batchQueryService.handle(query);

        return result.isPresent();
    }

    @Override
    public boolean belongsToLaboratory(Long batchId, Long laboratoryId) {
        var query = new GetBatchByIdQuery(batchId);

        var result = batchQueryService.handle(query);

        return result
                .map(batch -> batch.getLabId().equals(laboratoryId))
                .orElse(false);
    }

    @Override
    public boolean isBatchReleased(Long batchId) {
        var query = new GetBatchByIdQuery(batchId);

        var result = batchQueryService.handle(query);

        return result
                .map(batch -> batch.getStatus() == BatchStatus.RELEASED)
                .orElse(false);
    }

    @Override
    public boolean isBatchRejected(Long batchId) {
        var query = new GetBatchByIdQuery(batchId);

        var result = batchQueryService.handle(query);

        return result
                .map(batch -> batch.getStatus() == BatchStatus.REJECTED)
                .orElse(false);
    }

    @Override
    public Optional<BatchReference> findBatch(Long batchId) {
        if (batchId == null || batchId <= 0) return Optional.empty();
        return batchQueryService.handle(new GetBatchByIdQuery(batchId)).map(BatchContextFacadeImpl::toReference);
    }

    @Override
    public List<BatchReference> findBatches(Long laboratoryId) {
        if (laboratoryId == null || laboratoryId <= 0) return List.of();
        return batchQueryService.handle(new GetBatchesByLabIdQuery(laboratoryId)).stream()
                .map(BatchContextFacadeImpl::toReference).toList();
    }

    @Override
    public List<MaterialUse> findMaterialUses(Long batchId) {
        if (batchId == null || batchId <= 0) return List.of();
        return usageQueryService.handle(new GetRawMaterialUsageByBatchIdQuery(batchId)).stream()
                .map(BatchContextFacadeImpl::toMaterialUse).toList();
    }

    private static BatchReference toReference(Batch batch) {
        return new BatchReference(batch.getId(), batch.getLabId(), batch.getBatchNumber(), batch.getProductName(),
                batch.getQuantity(), batch.getUnit(), batch.getStatus() == null ? null : batch.getStatus().name(),
                batch.getStartDate(), batch.getEndDate(), batch.getNotes());
    }

    private static MaterialUse toMaterialUse(RawMaterialUsage usage) {
        return new MaterialUse(usage.getId(), usage.getRawMaterialId(), usage.getRawMaterialName(),
                usage.getInventoryReceiptId(), usage.getQuantityUsed(), usage.getUnit(), usage.getUsageDate(),
                usage.getStockBefore(), usage.getStockAfter());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TraceabilityReference> findTraceability(Long batchId) {
        if (batchId == null) return Optional.empty();
        return repository.findById(batchId)
                .filter(batch -> batch.getEnvironmentId() != null)
                .flatMap(batch -> traceabilityQueryService.handle(new GetBatchTraceabilityQuery(batch.getLabId(),
                        batch.getEnvironmentId(), batch.getProductId(), batch.getId())))
                .map(trace -> new TraceabilityReference(trace.batch().getId(), trace.product().getCode(),
                        trace.rawMaterials().stream().map(traced -> toMaterialUse(traced.usage())).toList(),
                        trace.equipment().stream().map(usage -> new EquipmentUse(usage.getEquipmentId(),
                                usage.getEquipmentName(), usage.getRegisteredAt())).toList(),
                        trace.staff().stream().map(member -> new StaffUse(member.getStaffId(), member.getStaffName(),
                                member.getStaffRole(), member.getRegisteredAt())).toList(),
                        trace.container().map(container -> new ContainerUse(container.containerMonitorId(),
                                container.containerName(), container.environmentId(), container.assignedAt())).orElse(null),
                        trace.release().map(signature -> new ReleaseSignature(signature.getSignedByUserId(),
                                signature.getSignatureHash(), signature.getSignedAt())).orElse(null),
                        trace.rejection().map(record -> new RejectionNote(record.getRejectionDate(), record.getReason()))
                                .orElse(null)));
    }
}
