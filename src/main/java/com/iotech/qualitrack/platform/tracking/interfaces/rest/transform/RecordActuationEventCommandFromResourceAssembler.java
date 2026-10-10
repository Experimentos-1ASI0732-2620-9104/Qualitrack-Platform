package com.iotech.qualitrack.platform.tracking.interfaces.rest.transform;

import com.iotech.qualitrack.platform.tracking.domain.model.commands.RecordActuationEventCommand;
import com.iotech.qualitrack.platform.tracking.interfaces.rest.resources.RecordActuationEventResource;

/**
 * Assembler that transforms an actuation event request into a command.
 */
public final class RecordActuationEventCommandFromResourceAssembler {

    private RecordActuationEventCommandFromResourceAssembler() {
    }

    public static RecordActuationEventCommand toCommandFromResource(Long laboratoryId, Long environmentId, Long deviceId,
                                                                    RecordActuationEventResource resource) {
        return new RecordActuationEventCommand(laboratoryId, environmentId, deviceId,
                TrackingRequestValues.action(resource.action()), TrackingRequestValues.optionalMetric(resource.triggerMetric()),
                TrackingRequestValues.optionalState(resource.triggerState()),
                TrackingRequestValues.optionalResult(resource.result()),
                TrackingRequestValues.instant(resource.occurredAt(), "occurredAt"), resource.profileVersion());
    }
}
