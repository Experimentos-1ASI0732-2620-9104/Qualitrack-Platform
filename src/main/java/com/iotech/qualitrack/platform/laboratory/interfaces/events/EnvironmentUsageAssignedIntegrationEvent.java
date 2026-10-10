package com.iotech.qualitrack.platform.laboratory.interfaces.events;

import com.iotech.qualitrack.platform.laboratory.domain.model.events.EnvironmentUsageAssignedEvent;

import java.time.Instant;

/**
 * Integration event published by the Laboratory bounded context when the main use of an environment is assigned.
 *
 * <p>Allows audit and other bounded contexts to react without depending on
 * Laboratory internal domain events.</p>
 */
public record EnvironmentUsageAssignedIntegrationEvent(
        Long environmentId,
        Long laboratoryId,
        String usage,
        Long assignedBy,
        Instant assignedAt
) {
    /**
     * Creates an integration event from an internal domain event.
     *
     * @param event the internal domain event
     * @return the integration event
     */
    public static EnvironmentUsageAssignedIntegrationEvent from(EnvironmentUsageAssignedEvent event) {
        return new EnvironmentUsageAssignedIntegrationEvent(
                event.environmentId(),
                event.laboratoryId(),
                event.usage().name(),
                event.assignedBy(),
                event.assignedAt()
        );
    }
}
