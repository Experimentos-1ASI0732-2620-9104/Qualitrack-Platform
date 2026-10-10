package com.iotech.qualitrack.platform.ra.domain.model.aggregates;

import com.iotech.qualitrack.platform.ra.domain.model.entities.KpiMetric;
import com.iotech.qualitrack.platform.ra.domain.model.valueobjects.KpiMetricStatus;
import com.iotech.qualitrack.platform.ra.domain.model.valueobjects.MeasurementSummary;
import com.iotech.qualitrack.platform.ra.domain.model.valueobjects.ReportingPeriod;
import com.iotech.qualitrack.platform.shared.domain.model.aggregates.AbstractDomainAggregateRoot;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Aggregate root representing a KPI dashboard snapshot for a laboratory.
 *
 * <p>The KPI dashboard groups multiple calculated KPI metrics and provides an
 * overall health score used by the reporting and analytics views. When it is calculated for a period it also
 * summarizes the environmental readings of that period (US93, TS81).</p>
 */
@Getter
public class KpiDashboard extends AbstractDomainAggregateRoot<KpiDashboard> {

    /**
     * The unique numeric identifier of the dashboard snapshot.
     */
    private Long id;

    /**
     * The numeric identifier of the laboratory associated with this dashboard.
     */
    private Long laboratoryId;

    /**
     * Timestamp when this dashboard snapshot was generated.
     */
    private String timestamp;

    /**
     * Overall health score calculated from all KPI metrics.
     */
    private Double overallHealthScore;

    /**
     * KPI metrics included in this dashboard snapshot.
     */
    private List<KpiMetric> metrics;

    /**
     * Period of the measurement summaries, or null for a snapshot without one.
     */
    private ReportingPeriod period;

    /**
     * Average, minimum and maximum of the readings of each device and metric in the period.
     */
    private List<MeasurementSummary> measurementSummaries = List.of();

    /**
     * Default constructor.
     * Required by the persistence and mapping layers to reconstruct the aggregate.
     */
    public KpiDashboard() {
        // Required for reconstruction by JPA or Assemblers
    }

    /**
     * Calculated indicators of a laboratory for a period: operational counts and measurement summaries.
     *
     * @param laboratoryId The laboratory identifier.
     * @param period The period of the measurement summaries.
     * @param metrics The operational counts.
     * @param measurementSummaries The summaries of the readings of the period.
     * @param timestamp When the indicators were calculated.
     */
    public KpiDashboard(Long laboratoryId, ReportingPeriod period, List<KpiMetric> metrics,
                        List<MeasurementSummary> measurementSummaries, String timestamp) {
        this(null, laboratoryId, timestamp, null, metrics);
        this.period = Objects.requireNonNull(period, "period cannot be null");
        this.measurementSummaries = List.copyOf(measurementSummaries);
    }

    /**
     * Reconstructs a KpiDashboard aggregate from persistence data.
     *
     * @param id The numeric dashboard identifier.
     * @param laboratoryId The laboratory identifier.
     * @param timestamp The generation timestamp.
     * @param overallHealthScore The calculated overall health score.
     * @param metrics The KPI metrics included in this snapshot.
     */
    public KpiDashboard(
            Long id,
            Long laboratoryId,
            String timestamp,
            Double overallHealthScore,
            List<KpiMetric> metrics
    ) {
        this.id = id;
        this.laboratoryId = laboratoryId;
        this.timestamp = timestamp;
        this.overallHealthScore = overallHealthScore;
        this.metrics = metrics;
    }

    /**
     * Creates a new KPI dashboard snapshot for a laboratory.
     *
     * @param laboratoryId The laboratory identifier.
     * @param metrics KPI metrics included in the dashboard.
     */
    public KpiDashboard(Long laboratoryId, List<KpiMetric> metrics) {
        this.laboratoryId = Objects.requireNonNull(laboratoryId, "laboratoryId cannot be null");
        this.metrics = Objects.requireNonNull(metrics, "metrics cannot be null");
        this.timestamp = LocalDateTime.now().toString();
        this.overallHealthScore = calculateOverallHealthScore(metrics);
    }

    /**
     * Calculates the overall health score using all KPI metric statuses.
     *
     * @param metrics KPI metrics to evaluate.
     * @return The calculated overall health score.
     */
    private Double calculateOverallHealthScore(List<KpiMetric> metrics) {
        if (metrics == null || metrics.isEmpty()) return 0.0;

        return metrics.stream()
                .mapToDouble(metric -> scoreFromStatus(metric.getStatus()))
                .average()
                .orElse(0.0);
    }

    /**
     * Converts a KPI metric status into a numeric health score.
     *
     * @param status The KPI metric status.
     * @return Numeric score associated with the status.
     */
    private Double scoreFromStatus(KpiMetricStatus status) {
        if (status == null) return 0.0;

        return switch (status) {
            case ON_TRACK -> 100.0;
            case AT_RISK -> 70.0;
            case CRITICAL -> 35.0;
            case UNKNOWN -> 0.0;
        };
    }

    /**
     * Indicates whether this dashboard contains critical KPI metrics.
     *
     * @return true if at least one metric is critical.
     */
    public boolean hasCriticalMetrics() {
        if (metrics == null || metrics.isEmpty()) return false;

        return metrics.stream()
                .anyMatch(metric -> metric.getStatus() == KpiMetricStatus.CRITICAL);
    }

    /**
     * Indicates whether this dashboard contains at-risk KPI metrics.
     *
     * @return true if at least one metric is at risk.
     */
    public boolean hasAtRiskMetrics() {
        if (metrics == null || metrics.isEmpty()) return false;

        return metrics.stream()
                .anyMatch(metric -> metric.getStatus() == KpiMetricStatus.AT_RISK);
    }
}