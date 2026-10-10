package com.iotech.qualitrack.platform.ra.interfaces.rest.transform;

import com.iotech.qualitrack.platform.ra.domain.model.aggregates.KpiDashboard;
import com.iotech.qualitrack.platform.ra.interfaces.rest.resources.KpiDashboardResource;
import com.iotech.qualitrack.platform.ra.interfaces.rest.resources.MeasurementSummaryResource;

/**
 * Assembler to convert KpiDashboard domain models into REST resources.
 */
public final class KpiDashboardResourceFromEntityAssembler {

    private KpiDashboardResourceFromEntityAssembler() {
    }

    public static KpiDashboardResource toResourceFromEntity(KpiDashboard entity) {
        return new KpiDashboardResource(
                entity.getId(),
                entity.getLaboratoryId(),
                entity.getTimestamp(),
                entity.getOverallHealthScore(),
                entity.getPeriod() == null ? null : entity.getPeriod().from(),
                entity.getPeriod() == null ? null : entity.getPeriod().to(),
                entity.getMetrics().stream()
                        .map(KpiMetricResourceFromEntityAssembler::toResourceFromEntity)
                        .toList(),
                entity.getMeasurementSummaries().stream()
                        .map(summary -> new MeasurementSummaryResource(summary.environmentId(), summary.deviceId(),
                                summary.metric(), summary.unit(), summary.readings(), summary.average(),
                                summary.minimum(), summary.maximum(), summary.firstMeasuredAt(), summary.lastMeasuredAt()))
                        .toList()
        );
    }
}
