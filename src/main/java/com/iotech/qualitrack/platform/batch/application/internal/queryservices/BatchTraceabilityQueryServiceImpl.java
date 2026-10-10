package com.iotech.qualitrack.platform.batch.application.internal.queryservices;

import com.iotech.qualitrack.platform.batch.application.internal.outboundservices.acl.BatchExternalEquipmentService;
import com.iotech.qualitrack.platform.batch.domain.model.valueobjects.BatchContainer;
import com.iotech.qualitrack.platform.batch.application.internal.outboundservices.acl.BatchExternalInventoryService;
import com.iotech.qualitrack.platform.batch.application.queryservices.BatchTraceabilityQueryService;
import com.iotech.qualitrack.platform.batch.domain.model.aggregates.Batch;
import com.iotech.qualitrack.platform.batch.domain.model.entities.RawMaterialUsage;
import com.iotech.qualitrack.platform.batch.domain.model.queries.GetBatchTraceabilityQuery;
import com.iotech.qualitrack.platform.batch.domain.model.valueobjects.BatchTraceability;
import com.iotech.qualitrack.platform.batch.domain.model.valueobjects.BatchTraceability.TracedRawMaterialUsage;
import com.iotech.qualitrack.platform.batch.domain.repositories.*;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Optional;

/**
 * Builds the traceability of a batch from its own records and the Inventory location of each raw material.
 */
@Service
public class BatchTraceabilityQueryServiceImpl implements BatchTraceabilityQueryService {

    private final BatchRepository batches;
    private final ProductRepository products;
    private final RawMaterialUsageRepository usages;
    private final BatchParticipationRepository participations;
    private final BatchEvidenceRepository evidence;
    private final BatchExternalInventoryService inventory;
    private final BatchExternalEquipmentService equipment;

    public BatchTraceabilityQueryServiceImpl(BatchRepository batches, ProductRepository products,
                                             RawMaterialUsageRepository usages, BatchParticipationRepository participations,
                                             BatchEvidenceRepository evidence, BatchExternalInventoryService inventory,
                                             BatchExternalEquipmentService equipment) {
        this.equipment = equipment;
        this.batches = batches;
        this.products = products;
        this.usages = usages;
        this.participations = participations;
        this.evidence = evidence;
        this.inventory = inventory;
    }

    @Override
    public Optional<BatchTraceability> handle(GetBatchTraceabilityQuery query) {
        return batches.findById(query.batchId())
                .filter(batch -> batch.belongsTo(query.laboratoryId(), query.environmentId(), query.productId()))
                .flatMap(batch -> products.findById(batch.getProductId()).map(product -> new BatchTraceability(
                        batch,
                        product,
                        tracedUsages(batch),
                        participations.findEquipmentUsagesByBatchId(batch.getId()),
                        participations.findStaffParticipationsByBatchId(batch.getId()),
                        evidence.findSignatureByBatchId(batch.getId()),
                        evidence.findRejectionByBatchId(batch.getId()),
                        batch.container().map(container -> new BatchContainer(batch.getId(), container.containerMonitorId(),
                                equipment.findContainerMonitor(batch.getLabId(), container.containerMonitorId())
                                        .map(reference -> reference.name()).orElse(null),
                                container.environmentId(), container.assignedBy(), container.assignedAt())))));
    }

    private java.util.List<TracedRawMaterialUsage> tracedUsages(Batch batch) {
        var environments = new HashMap<Long, Optional<Long>>();
        return usages.findAllByBatchId(batch.getId()).stream()
                .map(usage -> new TracedRawMaterialUsage(usage, environmentOf(batch, usage, environments)))
                .toList();
    }

    private Long environmentOf(Batch batch, RawMaterialUsage usage, HashMap<Long, Optional<Long>> cache) {
        // Usages without an Inventory lot point to pre-Inventory materials, which have no environment.
        if (usage.getInventoryReceiptId() == null) return null;
        return cache.computeIfAbsent(usage.getRawMaterialId(),
                material -> inventory.findRawMaterialEnvironment(batch.getLabId(), material)).orElse(null);
    }
}
