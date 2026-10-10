package com.iotech.qualitrack.platform.laboratory.interfaces.events;

import com.iotech.qualitrack.platform.laboratory.domain.model.events.EnvironmentUpdatedEvent;

/**
 * Integration event published by the Laboratory bounded context when the identification data of an environment changes.
 *
 * <p>Allows audit and other bounded contexts to react without depending on
 * Laboratory internal domain events.</p>
 */
public record EnvironmentUpdatedIntegrationEvent(
        Long environmentId,
        Long laboratoryId,
        String code,
        String name
) {
    /**
     * Creates an integration event from an internal domain event.
     *
     * @param event the internal domain event
     * @return the integration event
     */
    public static EnvironmentUpdatedIntegrationEvent from(EnvironmentUpdatedEvent event) {
        return new EnvironmentUpdatedIntegrationEvent(
                event.environmentId(),
                event.laboratoryId(),
                event.code(),
                event.name()
        );
    }
}
