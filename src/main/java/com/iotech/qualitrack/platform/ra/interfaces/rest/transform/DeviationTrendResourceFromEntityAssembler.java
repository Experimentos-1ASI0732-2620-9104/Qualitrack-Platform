package com.iotech.qualitrack.platform.ra.interfaces.rest.transform;

import com.iotech.qualitrack.platform.ra.domain.model.entities.DeviationTrend;
import com.iotech.qualitrack.platform.ra.interfaces.rest.resources.DeviationTrendResource;

/**
 * Assembler to convert DeviationTrend domain models into REST resources.
 */
public final class DeviationTrendResourceFromEntityAssembler {

    private DeviationTrendResourceFromEntityAssembler() {
    }

    public static DeviationTrendResource toResourceFromEntity(DeviationTrend entity) {
        return new DeviationTrendResource(
                entity.getId(),
                entity.getParameterName(),
                entity.getEquipmentId(),
                entity.getEnvironmentId(),
                entity.getUnit(),
                entity.getTrendDirection(),
                entity.getEvaluatedReadings(),
                entity.getTimeInRangePercent(),
                entity.getDeviationCount(),
                entity.getCriticalDeviationCount(),
                entity.getDataPoints().stream()
                        .map(TrendDataPointResourceFromEntityAssembler::toResourceFromEntity)
                        .toList()
        );
    }
}
