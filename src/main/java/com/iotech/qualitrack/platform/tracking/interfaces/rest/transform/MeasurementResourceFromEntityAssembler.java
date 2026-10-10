package com.iotech.qualitrack.platform.tracking.interfaces.rest.transform;

import com.iotech.qualitrack.platform.tracking.domain.model.entities.Measurement;
import com.iotech.qualitrack.platform.tracking.interfaces.rest.resources.MeasurementResource;

/**
 * Assembler that transforms readings into REST resources.
 */
public final class MeasurementResourceFromEntityAssembler {

    private MeasurementResourceFromEntityAssembler() {
    }

    public static MeasurementResource toResourceFromEntity(Measurement entity) {
        return new MeasurementResource(entity.getId(), entity.getEquipmentId(), entity.getEnvironmentId(),
                entity.getParameterName(), entity.getValue(), entity.getTextValue(), entity.getUnit(),
                entity.getMeasuredAt() == null ? entity.getTimestamp() : entity.getMeasuredAt().toString(),
                entity.getState() == null ? null : entity.getState().name(), entity.getThresholdValue(),
                entity.getProfileVersion());
    }
}
