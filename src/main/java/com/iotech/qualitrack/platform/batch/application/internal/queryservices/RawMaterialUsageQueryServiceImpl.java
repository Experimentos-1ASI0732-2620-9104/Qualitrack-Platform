package com.iotech.qualitrack.platform.batch.application.internal.queryservices;

import com.iotech.qualitrack.platform.batch.application.queryservices.RawMaterialUsageQueryService;
import com.iotech.qualitrack.platform.batch.domain.model.entities.RawMaterialUsage;
import com.iotech.qualitrack.platform.batch.domain.model.queries.GetRawMaterialUsageByBatchIdQuery;
import com.iotech.qualitrack.platform.batch.domain.repositories.RawMaterialUsageRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Application service implementation that resolves raw material usage read queries.
 */
@Service
public class RawMaterialUsageQueryServiceImpl implements RawMaterialUsageQueryService {

    private final RawMaterialUsageRepository rawMaterialUsageRepository;

    public RawMaterialUsageQueryServiceImpl(RawMaterialUsageRepository rawMaterialUsageRepository) {
        this.rawMaterialUsageRepository = rawMaterialUsageRepository;
    }

    @Override
    public List<RawMaterialUsage> handle(GetRawMaterialUsageByBatchIdQuery query) {
        return rawMaterialUsageRepository.findAllByBatchId(query.batchId());
    }

    @Override
    public List<RawMaterialUsage> handle(com.iotech.qualitrack.platform.batch.domain.model.queries.GetRawMaterialUsagesByInventoryMaterialQuery query) {
        return rawMaterialUsageRepository.findAllByRawMaterialId(query.rawMaterialId()).stream()
                .filter(usage -> usage.getInventoryReceiptId() != null)
                .sorted(java.util.Comparator.comparing(RawMaterialUsage::getId).reversed())
                .toList();
    }

    @Override
    public List<RawMaterialUsage> handle(com.iotech.qualitrack.platform.batch.domain.model.queries.GetRawMaterialHistoryQuery query) {
        return rawMaterialUsageRepository.findAllByRawMaterialId(query.rawMaterialId()).stream()
                .filter(usage -> usage.getInventoryReceiptId() == null)
                .sorted(java.util.Comparator.comparing(RawMaterialUsage::getId).reversed())
                .toList();
    }
}
