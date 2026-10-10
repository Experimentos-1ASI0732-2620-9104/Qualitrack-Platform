package com.iotech.qualitrack.platform.tracking.interfaces.rest.transform;

import com.iotech.qualitrack.platform.tracking.domain.model.entities.ActuationEvent;
import com.iotech.qualitrack.platform.tracking.interfaces.rest.resources.ActuationEventResource;

/**
 * Assembler that transforms actuation events into REST resources.
 */
public final class ActuationEventResourceFromEntityAssembler {

    private ActuationEventResourceFromEntityAssembler() {
    }

    public static ActuationEventResource toResourceFromEntity(ActuationEvent entity) {
        return new ActuationEventResource(entity.getId(), entity.getDeviceId(), entity.getEnvironmentId(),
                entity.getAction().name(), entity.getTriggerMetric() == null ? null : entity.getTriggerMetric().name(),
                entity.getTriggerState() == null ? null : entity.getTriggerState().name(), entity.getResult().name(),
                entity.getOccurredAt().toString(), entity.getProfileVersion());
    }
}
