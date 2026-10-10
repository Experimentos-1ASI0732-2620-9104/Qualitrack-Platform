package com.iotech.qualitrack.platform.batch.application.internal.commandservices;

import com.iotech.qualitrack.platform.batch.application.commandservices.BatchCommandService;
import com.iotech.qualitrack.platform.batch.application.internal.outboundservices.acl.BatchExternalEquipmentService;
import com.iotech.qualitrack.platform.batch.application.internal.outboundservices.acl.ExternalLaboratoryService;
import com.iotech.qualitrack.platform.batch.domain.model.commands.AssignBatchContainerCommand;
import com.iotech.qualitrack.platform.batch.domain.model.events.BatchStoredInContainerEvent;
import com.iotech.qualitrack.platform.batch.domain.model.valueobjects.BatchContainer;
import com.iotech.qualitrack.platform.batch.domain.model.valueobjects.ContainerAssignment;
import com.iotech.qualitrack.platform.batch.domain.model.aggregates.Batch;
import com.iotech.qualitrack.platform.batch.domain.model.commands.CreateBatchCommand;
import com.iotech.qualitrack.platform.batch.domain.model.commands.RejectBatchCommand;
import com.iotech.qualitrack.platform.batch.domain.model.commands.ReleaseBatchCommand;
import com.iotech.qualitrack.platform.batch.domain.model.entities.DigitalSignature;
import com.iotech.qualitrack.platform.batch.domain.model.entities.RejectionRecord;
import com.iotech.qualitrack.platform.batch.domain.model.events.BatchCreatedEvent;
import com.iotech.qualitrack.platform.batch.domain.model.events.BatchRejectedEvent;
import com.iotech.qualitrack.platform.batch.domain.model.events.BatchReleasedEvent;
import com.iotech.qualitrack.platform.batch.domain.model.valueobjects.BatchRejection;
import com.iotech.qualitrack.platform.batch.domain.model.valueobjects.BatchRelease;
import com.iotech.qualitrack.platform.batch.domain.repositories.BatchEvidenceRepository;
import com.iotech.qualitrack.platform.batch.domain.repositories.BatchRepository;
import com.iotech.qualitrack.platform.batch.domain.repositories.ProductRepository;
import com.iotech.qualitrack.platform.shared.application.result.ApplicationError;
import com.iotech.qualitrack.platform.shared.application.result.Result;
import com.iotech.qualitrack.platform.shared.application.security.CurrentUser;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * Registers product batches and records their release or rejection.
 */
@Service
public class BatchCommandServiceImpl implements BatchCommandService {

    private final BatchRepository batchRepository;
    private final ProductRepository productRepository;
    private final BatchEvidenceRepository evidenceRepository;
    private final CurrentUser currentUser;
    private final ApplicationEventPublisher eventPublisher;
    private final BatchExternalEquipmentService equipment;
    private final ExternalLaboratoryService laboratory;

    public BatchCommandServiceImpl(BatchRepository batchRepository, ProductRepository productRepository,
                                   BatchEvidenceRepository evidenceRepository, CurrentUser currentUser,
                                   ApplicationEventPublisher eventPublisher, BatchExternalEquipmentService equipment,
                                   ExternalLaboratoryService laboratory) {
        this.equipment = equipment;
        this.laboratory = laboratory;
        this.batchRepository = batchRepository;
        this.productRepository = productRepository;
        this.evidenceRepository = evidenceRepository;
        this.currentUser = currentUser;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public Result<Batch, ApplicationError> handle(CreateBatchCommand command) {
        var product = productRepository.findById(command.productId())
                .filter(value -> value.belongsTo(command.laboratoryId(), command.environmentId()));
        if (product.isEmpty()) {
            return Result.failure(ApplicationError.notFound("PharmaceuticalProduct", command.productId()));
        }
        if (batchRepository.existsByLabIdAndBatchNumber(command.laboratoryId(), command.batchNumber())) {
            return Result.failure(ApplicationError.conflict("Batch",
                    "Batch with number '%s' already exists in this laboratory".formatted(command.batchNumber())));
        }
        var batch = batchRepository.save(new Batch(command, product.get()));
        eventPublisher.publishEvent(BatchCreatedEvent.from(batch));
        return Result.success(batch);
    }

    @Override
    @Transactional
    public Result<BatchRelease, ApplicationError> handle(ReleaseBatchCommand command) {
        var batch = lockedBatch(command.laboratoryId(), command.environmentId(), command.productId(), command.batchId());
        if (batch.isEmpty()) return Result.failure(ApplicationError.notFound("Batch", command.batchId()));
        try {
            batch.get().release(command);
        } catch (IllegalStateException exception) {
            return Result.failure(ApplicationError.conflict("Batch", exception.getMessage()));
        }
        var released = batchRepository.save(batch.get());
        var signature = evidenceRepository.saveSignature(
                DigitalSignature.sign(released, currentUser.userId(), Instant.now().truncatedTo(ChronoUnit.SECONDS)));
        eventPublisher.publishEvent(BatchReleasedEvent.from(released, currentUser.userId()));
        return Result.success(new BatchRelease(released, signature));
    }

    @Override
    @Transactional
    public Result<BatchRejection, ApplicationError> handle(RejectBatchCommand command) {
        var batch = lockedBatch(command.laboratoryId(), command.environmentId(), command.productId(), command.batchId());
        if (batch.isEmpty()) return Result.failure(ApplicationError.notFound("Batch", command.batchId()));
        try {
            batch.get().reject(command);
        } catch (IllegalStateException exception) {
            return Result.failure(ApplicationError.conflict("Batch", exception.getMessage()));
        }
        var rejected = batchRepository.save(batch.get());
        var record = evidenceRepository.saveRejection(new RejectionRecord(command));
        eventPublisher.publishEvent(BatchRejectedEvent.from(rejected, currentUser.userId()));
        return Result.success(new BatchRejection(rejected, record));
    }

    /**
     * Stores the batch in an operational container monitor located in a product storage environment of the laboratory.
     * Storing it again in the same container changes nothing.
     */
    @Override
    @Transactional
    public Result<BatchContainer, ApplicationError> handle(AssignBatchContainerCommand command) {
        var batch = lockedBatch(command.laboratoryId(), command.environmentId(), command.productId(), command.batchId());
        if (batch.isEmpty()) return Result.failure(ApplicationError.notFound("Batch", command.batchId()));
        if (command.containerMonitorId() == null) {
            return Result.failure(ApplicationError.validationError("containerMonitorId", "Container monitor is required"));
        }
        var container = equipment.findContainerMonitor(command.laboratoryId(), command.containerMonitorId());
        if (container.isEmpty()) {
            return Result.failure(ApplicationError.conflict("ContainerAssignment", "The container monitor is not registered in the laboratory"));
        }
        var reference = container.get();
        if (!reference.isLocated()) {
            return Result.failure(ApplicationError.conflict("ContainerAssignment", "The container is not located in an environment"));
        }
        var environment = laboratory.findEnvironment(command.laboratoryId(), reference.environmentId());
        if (environment.isEmpty() || !environment.get().hasUsage("PRODUCT_STORAGE")) {
            return Result.failure(ApplicationError.conflict("ContainerAssignment",
                    "Product batches are stored in containers of a product storage environment"));
        }
        if (!reference.isAvailable()) {
            return Result.failure(ApplicationError.conflict("ContainerAssignment",
                    "Container '%s' is %s and cannot receive batches".formatted(reference.name(), reference.status())));
        }
        var stored = batch.get();
        if (stored.storeIn(new ContainerAssignment(reference.id(), reference.environmentId(), currentUser.userId(),
                Instant.now().truncatedTo(ChronoUnit.SECONDS)))) {
            stored = batchRepository.save(stored);
            eventPublisher.publishEvent(new BatchStoredInContainerEvent(stored.getId(), stored.getLabId(), reference.id(),
                    reference.name(), reference.environmentId()));
        }
        var assignment = stored.container().orElseThrow();
        return Result.success(new BatchContainer(stored.getId(), assignment.containerMonitorId(), reference.name(),
                assignment.environmentId(), assignment.assignedBy(), assignment.assignedAt()));
    }

    private Optional<Batch> lockedBatch(Long laboratoryId, Long environmentId, Long productId, Long batchId) {
        return batchRepository.findByIdForUpdate(batchId)
                .filter(batch -> batch.belongsTo(laboratoryId, environmentId, productId));
    }
}
