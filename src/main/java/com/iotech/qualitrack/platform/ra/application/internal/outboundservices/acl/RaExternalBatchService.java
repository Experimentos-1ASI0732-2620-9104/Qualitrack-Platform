package com.iotech.qualitrack.platform.ra.application.internal.outboundservices.acl;

import com.iotech.qualitrack.platform.batch.interfaces.acl.BatchContextFacade;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Anti-corruption service that reads, through Product Batch Management, the batches and their traceability.
 */
@Service
public class RaExternalBatchService {
    private final BatchContextFacade batchContextFacade;

    public RaExternalBatchService(BatchContextFacade batchContextFacade) {
        this.batchContextFacade = batchContextFacade;
    }

    /**
     * A product batch, or empty when it does not exist.
     */
    public Optional<BatchContextFacade.BatchReference> findBatch(Long batchId) {
        return batchContextFacade.findBatch(batchId);
    }

    /**
     * Product batches of the laboratory.
     */
    public List<BatchContextFacade.BatchReference> findBatches(Long laboratoryId) {
        return batchContextFacade.findBatches(laboratoryId);
    }

    /**
     * Raw materials consumed by a batch as they were recorded, also for older batches without environment.
     */
    public List<BatchContextFacade.MaterialUse> findMaterialUses(Long batchId) {
        return batchContextFacade.findMaterialUses(batchId);
    }

    /**
     * Lots, equipment, staff, container and quality decision of a batch; empty for older batches without environment.
     */
    public Optional<BatchContextFacade.TraceabilityReference> findTraceability(Long batchId) {
        return batchContextFacade.findTraceability(batchId);
    }
}
