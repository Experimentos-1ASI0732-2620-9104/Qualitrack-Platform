package com.iotech.qualitrack.platform.ra.interfaces.rest.transform;

import com.iotech.qualitrack.platform.ra.domain.model.entities.TrendDataPoint;
import com.iotech.qualitrack.platform.ra.interfaces.rest.resources.TrendDataPointResource;

/**
 * Assembler to convert TrendDataPoint domain models into REST resources.
 */
public final class TrendDataPointResourceFromEntityAssembler {

    private TrendDataPointResourceFromEntityAssembler() {
    }

    public static TrendDataPointResource toResourceFromEntity(TrendDataPoint entity) {
        return new TrendDataPointResource(
                entity.getTimestamp(),
                entity.getRecordedValue(),
                entity.getUpperThreshold(),
                entity.getLowerThreshold(),
                entity.getState()
        );
    }
}
