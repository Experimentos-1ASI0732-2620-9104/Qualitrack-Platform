package com.iotech.qualitrack.platform.inventory.application.internal.outboundservices;
import com.iotech.qualitrack.platform.inventory.domain.model.aggregates.RawMaterialBatch;
import com.iotech.qualitrack.platform.inventory.domain.model.entities.InventoryMovement;
import com.iotech.qualitrack.platform.inventory.domain.repositories.InventoryRepository;
import com.iotech.qualitrack.platform.inventory.interfaces.events.InventoryMovementRecordedIntegrationEvent;
import com.iotech.qualitrack.platform.shared.application.security.CurrentUser;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.math.BigDecimal;
import java.time.Clock;

@Service
public class InventoryMovementRecorder {
    private final InventoryRepository repository;
    private final CurrentUser actor;
    private final Clock clock;
    private final ApplicationEventPublisher events;
    public InventoryMovementRecorder(InventoryRepository repository, CurrentUser actor, Clock inventoryClock,
                                     ApplicationEventPublisher events) {
        this.events = events;
        this.repository = repository;
        this.actor = actor;
        this.clock = inventoryClock;
    }
    @Transactional(propagation = Propagation.MANDATORY)
    public InventoryMovement record(RawMaterialBatch receipt, Long batch, String type, BigDecimal amount,
            BigDecimal before, String previousStatus, String reason, String operationId) {
        var user = actor.userId();
        if (user == null) throw new org.springframework.security.access.AccessDeniedException("Authenticated reviewer is required");
        var movement = repository.append(new InventoryMovement(null, receipt.getLaboratoryId(), receipt.getRawMaterialId(), receipt.getId(),
            batch, type, amount, receipt.getUnit(), before, receipt.getAvailableAmount(), previousStatus,
            receipt.getStatus().name(), reason, user, clock.instant(), operationId));
        events.publishEvent(new InventoryMovementRecordedIntegrationEvent(movement.id(), movement.laboratoryId(),
                movement.materialId(), movement.receiptId(), movement.type(), movement.statusAfter(), movement.actorId(),
                movement.occurredAt()));
        return movement;
    }
}
