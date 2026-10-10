package com.iotech.qualitrack.platform.laboratory.interfaces.events;

import com.iotech.qualitrack.platform.laboratory.domain.model.events.EnvironmentRegisteredEvent;

/**
 * Integration event published by the Laboratory bounded context when an environment is registered.
 *
 * <p>Allows audit and other bounded contexts to react without depending on
 * Laboratory internal domain events.</p>
 */
public record EnvironmentRegisteredIntegrationEvent(
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
    public static EnvironmentRegisteredIntegrationEvent from(EnvironmentRegisteredEvent event) {
        return new EnvironmentRegisteredIntegrationEvent(
                event.environmentId(),
                event.laboratoryId(),
                event.code(),
                event.name()
        );
    }
}
