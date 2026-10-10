package com.iotech.qualitrack.platform.batch.application.internal.commandservices;

import com.iotech.qualitrack.platform.batch.application.commandservices.RawMaterialUsageCommandService;
import com.iotech.qualitrack.platform.batch.application.internal.outboundservices.acl.BatchExternalInventoryService;
import com.iotech.qualitrack.platform.batch.domain.model.commands.RegisterRawMaterialUsageCommand;
import com.iotech.qualitrack.platform.batch.domain.model.entities.RawMaterialUsage;
import com.iotech.qualitrack.platform.batch.domain.repositories.BatchRepository;
import com.iotech.qualitrack.platform.batch.domain.repositories.RawMaterialUsageRepository;
import com.iotech.qualitrack.platform.shared.application.result.ApplicationError;
import com.iotech.qualitrack.platform.shared.application.result.Result;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registers raw material usages of product batches.
 *
 * <p>Product Batch Management does not manage stock: it asks Inventory Management to consume the lot.
 * Inventory publishes the confirmed consumption and {@code ReceiptConsumedEventHandler} records the usage
 * in the same transaction, so stock and traceability never diverge.</p>
 */
@Service
public class RawMaterialUsageCommandServiceImpl implements RawMaterialUsageCommandService {

    private final BatchRepository batches;
    private final RawMaterialUsageRepository usages;
    private final BatchExternalInventoryService inventory;

    public RawMaterialUsageCommandServiceImpl(BatchRepository batches, RawMaterialUsageRepository usages,
                                              BatchExternalInventoryService inventory) {
        this.batches = batches;
        this.usages = usages;
        this.inventory = inventory;
    }

    @Override
    @Transactional
    public Result<RawMaterialUsage, ApplicationError> handle(RegisterRawMaterialUsageCommand command) {
        var batch = batches.findById(command.batchId())
                .filter(value -> value.belongsTo(command.laboratoryId(), command.environmentId(), command.productId()));
        if (batch.isEmpty()) return Result.failure(ApplicationError.notFound("Batch", command.batchId()));
        var previous = usages.findByBatchIdAndOperationId(command.batchId(), command.operationId());
        if (previous.isPresent()) return Result.success(previous.get());
        if (!batch.get().isOpen()) {
            return Result.failure(ApplicationError.conflict("Batch", "Released or rejected batches cannot consume raw materials"));
        }
        inventory.consume(command.laboratoryId(), command.rawMaterialBatchId(), command.batchId(),
                command.amountUsed(), command.unit(), command.operationId());
        return usages.findByBatchIdAndOperationId(command.batchId(), command.operationId())
                .<Result<RawMaterialUsage, ApplicationError>>map(Result::success)
                .orElseGet(() -> Result.failure(ApplicationError.unexpected("register-raw-material-usage",
                        "Inventory confirmed the consumption but the usage was not recorded")));
    }
}
