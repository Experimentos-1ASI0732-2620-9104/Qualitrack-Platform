package com.iotech.qualitrack.platform.batch.application.internal.commandservices;

import com.iotech.qualitrack.platform.batch.application.commandservices.BatchParticipationCommandService;
import com.iotech.qualitrack.platform.batch.application.internal.outboundservices.acl.BatchExternalEquipmentService;
import com.iotech.qualitrack.platform.batch.application.internal.outboundservices.acl.ExternalLaboratoryService;
import com.iotech.qualitrack.platform.batch.domain.model.aggregates.Batch;
import com.iotech.qualitrack.platform.batch.domain.model.commands.RegisterEquipmentUsageCommand;
import com.iotech.qualitrack.platform.batch.domain.model.commands.RegisterStaffParticipationCommand;
import com.iotech.qualitrack.platform.batch.domain.model.entities.EquipmentUsage;
import com.iotech.qualitrack.platform.batch.domain.model.entities.StaffParticipation;
import com.iotech.qualitrack.platform.batch.domain.model.events.BatchParticipationRegisteredEvent;
import com.iotech.qualitrack.platform.batch.domain.repositories.BatchParticipationRepository;
import com.iotech.qualitrack.platform.batch.domain.repositories.BatchRepository;
import com.iotech.qualitrack.platform.shared.application.result.ApplicationError;
import com.iotech.qualitrack.platform.shared.application.result.Result;
import com.iotech.qualitrack.platform.shared.application.security.CurrentUser;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * Associates equipment (validated with Equipment Management) and staff (validated with Laboratory
 * Management) with product batches.
 */
@Service
public class BatchParticipationCommandServiceImpl implements BatchParticipationCommandService {

    private static final String CLOSED_BATCH = "Released or rejected batches cannot register new participants";

    private final BatchRepository batches;
    private final BatchParticipationRepository participations;
    private final BatchExternalEquipmentService equipment;
    private final ExternalLaboratoryService laboratory;
    private final CurrentUser currentUser;
    private final ApplicationEventPublisher events;

    public BatchParticipationCommandServiceImpl(BatchRepository batches, BatchParticipationRepository participations,
                                                BatchExternalEquipmentService equipment, ExternalLaboratoryService laboratory,
                                                CurrentUser currentUser, ApplicationEventPublisher events) {
        this.batches = batches;
        this.participations = participations;
        this.equipment = equipment;
        this.laboratory = laboratory;
        this.currentUser = currentUser;
        this.events = events;
    }

    @Override
    @Transactional
    public Result<EquipmentUsage, ApplicationError> handle(RegisterEquipmentUsageCommand command) {
        var batch = openBatch(command.laboratoryId(), command.environmentId(), command.productId(), command.batchId());
        if (batch.isFailure()) return batch.map(value -> null);
        var reference = equipment.findEquipment(command.laboratoryId(), command.equipmentId());
        if (reference.isEmpty()) return Result.failure(ApplicationError.notFound("Equipment", command.equipmentId()));
        if (!reference.get().isAvailable()) {
            return Result.failure(ApplicationError.conflict("Equipment",
                    "Equipment '%s' is %s and cannot be used".formatted(reference.get().name(), reference.get().status())));
        }
        if (participations.existsEquipmentUsage(command.batchId(), command.equipmentId())) {
            return Result.failure(ApplicationError.conflict("EquipmentUsage", "The equipment is already associated with the batch"));
        }
        var usage = participations.saveEquipmentUsage(new EquipmentUsage(command.batchId(), command.equipmentId(),
                reference.get().name(), currentUser.userId(), now()));
        events.publishEvent(new BatchParticipationRegisteredEvent(command.batchId(), command.laboratoryId(),
                BatchParticipationRegisteredEvent.EQUIPMENT, usage.getEquipmentId(), usage.getEquipmentName()));
        return Result.success(usage);
    }

    @Override
    @Transactional
    public Result<StaffParticipation, ApplicationError> handle(RegisterStaffParticipationCommand command) {
        var batch = openBatch(command.laboratoryId(), command.environmentId(), command.productId(), command.batchId());
        if (batch.isFailure()) return batch.map(value -> null);
        var member = laboratory.findStaffMember(command.laboratoryId(), command.staffId());
        if (member.isEmpty()) return Result.failure(ApplicationError.notFound("StaffMember", command.staffId()));
        if (!member.get().active()) {
            return Result.failure(ApplicationError.conflict("StaffMember", "Inactive staff members cannot take part in a batch"));
        }
        if (member.get().isAuditor()) {
            return Result.failure(ApplicationError.validationError("staffId", "Auditors only consult the records and cannot take part in a batch"));
        }
        if (!currentUser.managesQuality() && !member.get().isAccount(currentUser.userId())) {
            throw new AccessDeniedException("Staff members can only assign themselves to a batch");
        }
        if (participations.existsStaffParticipation(command.batchId(), command.staffId())) {
            return Result.failure(ApplicationError.conflict("StaffParticipation", "The staff member is already associated with the batch"));
        }
        var participation = participations.saveStaffParticipation(new StaffParticipation(command.batchId(), command.staffId(),
                member.get().fullName(), member.get().role(), currentUser.userId(), now()));
        events.publishEvent(new BatchParticipationRegisteredEvent(command.batchId(), command.laboratoryId(),
                BatchParticipationRegisteredEvent.STAFF, participation.getStaffId(), participation.getStaffName()));
        return Result.success(participation);
    }

    private Result<Batch, ApplicationError> openBatch(Long laboratoryId, Long environmentId, Long productId, Long batchId) {
        Optional<Batch> batch = batches.findById(batchId).filter(value -> value.belongsTo(laboratoryId, environmentId, productId));
        if (batch.isEmpty()) return Result.failure(ApplicationError.notFound("Batch", batchId));
        if (!batch.get().isOpen()) return Result.failure(ApplicationError.conflict("Batch", CLOSED_BATCH));
        return Result.success(batch.get());
    }

    private static String now() {
        return Instant.now().truncatedTo(ChronoUnit.SECONDS).toString();
    }
}
