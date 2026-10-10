package com.iotech.qualitrack.platform.batch.application.acl;

import com.iotech.qualitrack.platform.batch.domain.repositories.BatchRepository;
import com.iotech.qualitrack.platform.shared.application.result.ApplicationException;
import com.iotech.qualitrack.platform.shared.application.result.ApplicationError;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import com.iotech.qualitrack.platform.batch.application.queryservices.BatchQueryService;
import com.iotech.qualitrack.platform.batch.application.queryservices.BatchTraceabilityQueryService;
import com.iotech.qualitrack.platform.batch.domain.model.queries.GetBatchTraceabilityQuery;
import com.iotech.qualitrack.platform.batch.domain.model.queries.GetBatchByIdQuery;
import com.iotech.qualitrack.platform.batch.domain.model.valueobjects.BatchStatus;
import com.iotech.qualitrack.platform.batch.interfaces.acl.BatchContextFacade;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Application-layer implementation of the Batch ACL facade.
 */
@Service
public class BatchContextFacadeImpl implements BatchContextFacade {

    private final BatchQueryService batchQueryService;
    private final BatchRepository repository;
    private final BatchTraceabilityQueryService traceabilityQueryService;

    /**
     * @param traceabilityQueryService lazy: the traceability reads Inventory, whose consumption validates batches here
     */
    public BatchContextFacadeImpl(BatchQueryService batchQueryService,
            BatchRepository repository, @Lazy BatchTraceabilityQueryService traceabilityQueryService) {
        this.batchQueryService = batchQueryService;
        this.repository = repository;
        this.traceabilityQueryService = traceabilityQueryService;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void requireConsumable(Long batchId, Long laboratoryId) {
        var batch = repository.findByIdForUpdate(batchId).filter(value -> laboratoryId.equals(value.getLabId()))
            .orElseThrow(() -> new ApplicationException(
                ApplicationError.notFound("Batch", batchId)));
        if (batch.getStatus() == BatchStatus.RELEASED || batch.getStatus() == BatchStatus.REJECTED)
            throw new ApplicationException(
                ApplicationError.conflict("Batch", "Closed product batches cannot consume materials"));
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
    @Transactional(readOnly = true)
    public Optional<TraceabilityReference> findTraceability(Long batchId) {
        if (batchId == null) return Optional.empty();
        return repository.findById(batchId)
                .filter(batch -> batch.getEnvironmentId() != null)
                .flatMap(batch -> traceabilityQueryService.handle(new GetBatchTraceabilityQuery(batch.getLabId(),
                        batch.getEnvironmentId(), batch.getProductId(), batch.getId())))
                .map(trace -> new TraceabilityReference(trace.batch().getId(), trace.product().getCode(),
                        trace.rawMaterials().stream().map(traced -> new MaterialUse(traced.usage().getId(),
                                traced.usage().getRawMaterialId(), traced.usage().getRawMaterialName(),
                                traced.usage().getInventoryReceiptId(), traced.usage().getQuantityUsed(),
                                traced.usage().getUnit(), traced.usage().getUsageDate(), traced.usage().getStockBefore(),
                                traced.usage().getStockAfter())).toList(),
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
