package com.iotech.qualitrack.platform.tracking.interfaces.rest.transform;

import com.iotech.qualitrack.platform.tracking.domain.model.commands.RecordMeasurementCommand;
import com.iotech.qualitrack.platform.tracking.interfaces.rest.resources.RecordMeasurementResource;

/**
 * Assembler that transforms a reading request into a command.
 */
public final class RecordMeasurementCommandFromResourceAssembler {

    private RecordMeasurementCommandFromResourceAssembler() {
    }

    /**
     * @param deviceId container monitor, or null for the environmental device of the environment
     */
    public static RecordMeasurementCommand toCommandFromResource(Long laboratoryId, Long environmentId, Long deviceId,
                                                                 RecordMeasurementResource resource) {
        return new RecordMeasurementCommand(laboratoryId, environmentId, deviceId,
                TrackingRequestValues.metric(resource.metric()), resource.value(), resource.textValue(),
                TrackingRequestValues.instant(resource.measuredAt(), "measuredAt"), resource.profileVersion());
    }
}
