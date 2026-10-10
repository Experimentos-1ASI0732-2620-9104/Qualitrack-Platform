package com.iotech.qualitrack.platform.ra.application.internal.outboundservices.acl;

import com.iotech.qualitrack.platform.batch.interfaces.acl.BatchContextFacade;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Anti-corruption service that reads, through Product Batch Management, the traceability of a batch.
 */
@Service
public class RaExternalBatchService {
    private final BatchContextFacade batchContextFacade;

    public RaExternalBatchService(BatchContextFacade batchContextFacade) {
        this.batchContextFacade = batchContextFacade;
    }

    /**
     * Lots, equipment, staff, container and quality decision of a batch; empty for older batches without environment.
     */
    public Optional<BatchContextFacade.TraceabilityReference> findTraceability(Long batchId) {
        return batchContextFacade.findTraceability(batchId);
    }
}
