package com.iotech.qualitrack.platform.ra.domain.model.entities;

import com.iotech.qualitrack.platform.ra.domain.model.valueobjects.EnvironmentalReading;
import com.iotech.qualitrack.platform.ra.domain.model.valueobjects.TrendDirection;
import lombok.Getter;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Domain entity representing a deviation trend analysis for a monitored variable of a device.
 *
 * <p>A trend groups the readings of one variable of an environmental device or container monitor in a period and
 * exposes its direction and its deviation indicators (US94, TS82):</p>
 * <ul>
 *   <li><b>Time in range</b>: each reading evaluated by the platform keeps its condition until the next evaluated
 *   reading; the percentage is the time spent in NORMAL over the time between the first and the last evaluated
 *   reading of the period (decision of 2026-10-04). With fewer than two evaluated readings it is unknown.</li>
 *   <li><b>Deviations</b>: readings whose condition is worse than the previous evaluated reading (the rule with which
 *   Tracking reports deviations to Compliance); a period that starts deviated counts once. Critical deviations are
 *   the readings that entered CRITICAL.</li>
 * </ul>
 */
@Getter
public class DeviationTrend {

    /**
     * The unique numeric identifier of the trend analysis.
     */
    private Long id;

    /**
     * The monitored parameter name.
     */
    private String parameterName;

    /**
     * The device or equipment whose readings form the trend.
     */
    private Long equipmentId;

    /**
     * The environment of the device, when the trend comes from environmental readings.
     */
    private Long environmentId;

    /**
     * The unit of the readings.
     */
    private String unit;

    /**
     * The calculated direction of the parameter trend.
     */
    private TrendDirection trendDirection;

    /**
     * The data points used to calculate the trend.
     */
    private List<TrendDataPoint> dataPoints;

    /**
     * Readings evaluated against the thresholds of the profile.
     */
    private int evaluatedReadings;

    /**
     * Percentage of time in NORMAL, or null when it cannot be calculated.
     */
    private Double timeInRangePercent;

    /**
     * Readings that moved the variable to a worse condition.
     */
    private int deviationCount;

    /**
     * Readings that moved the variable to CRITICAL.
     */
    private int criticalDeviationCount;

    /**
     * Default constructor.
     * Required by the persistence and mapping layers to reconstruct the entity.
     */
    public DeviationTrend() {
        // Required for reconstruction by JPA or Assemblers
    }

    /**
     * Reconstructs a DeviationTrend entity from persistence data.
     *
     * @param id The numeric trend ID.
     * @param parameterName The monitored parameter name.
     * @param equipmentId The equipment ID.
     * @param trendDirection The calculated trend direction.
     * @param dataPoints The data points of the trend.
     */
    public DeviationTrend(
            Long id,
            String parameterName,
            Long equipmentId,
            TrendDirection trendDirection,
            List<TrendDataPoint> dataPoints
    ) {
        this.id = id;
        this.parameterName = parameterName;
        this.equipmentId = equipmentId;
        this.trendDirection = trendDirection;
        this.dataPoints = dataPoints;
    }

    /**
     * Creates a new DeviationTrend from a list of data points.
     *
     * @param parameterName The monitored parameter name.
     * @param equipmentId The equipment ID.
     * @param dataPoints The data points of the trend.
     */
    public DeviationTrend(
            String parameterName,
            Long equipmentId,
            List<TrendDataPoint> dataPoints
    ) {
        this.parameterName = Objects.requireNonNull(parameterName, "parameterName cannot be null");
        this.equipmentId = Objects.requireNonNull(equipmentId, "equipmentId cannot be null");
        this.dataPoints = Objects.requireNonNull(dataPoints, "dataPoints cannot be null");
        this.trendDirection = calculateTrendDirection(dataPoints);
    }

    /**
     * Calculates the trend and the deviation indicators of one variable of a device from its readings.
     *
     * @param environmentId the environment of the device
     * @param deviceId the environmental device or container monitor
     * @param metric the variable
     * @param unit the unit of the readings
     * @param readings readings of the device and variable in the period, in any order
     * @return the trend; its data points only include numeric readings
     */
    public static DeviationTrend fromReadings(Long environmentId, Long deviceId, String metric, String unit,
                                              List<EnvironmentalReading> readings) {
        var numeric = readings.stream().filter(EnvironmentalReading::isNumeric)
                .sorted(Comparator.comparing(EnvironmentalReading::measuredAt)).toList();
        var trend = new DeviationTrend(metric, deviceId, numeric.stream()
                .map(reading -> new TrendDataPoint(reading.measuredAt().toString(), reading.value(), null, null,
                        reading.state()))
                .toList());
        trend.environmentId = environmentId;
        trend.unit = unit;

        var evaluated = numeric.stream().filter(EnvironmentalReading::isEvaluated).toList();
        trend.evaluatedReadings = evaluated.size();
        var total = Duration.ZERO;
        var inRange = Duration.ZERO;
        EnvironmentalReading previous = null;
        for (var reading : evaluated) {
            var previousRank = previous == null ? 0 : previous.severityRank();
            if (reading.severityRank() > 0 && reading.severityRank() > previousRank) trend.deviationCount++;
            if (reading.severityRank() == 2 && previousRank != 2) trend.criticalDeviationCount++;
            if (previous != null) {
                var interval = Duration.between(previous.measuredAt(), reading.measuredAt());
                total = total.plus(interval);
                if (previous.severityRank() == 0) inRange = inRange.plus(interval);
            }
            previous = reading;
        }
        trend.timeInRangePercent = total.isZero() ? null
                : Math.round(inRange.toMillis() * 1000.0 / total.toMillis()) / 10.0;
        return trend;
    }

    /**
     * Calculates the direction of the trend based on the first and last values.
     *
     * @param dataPoints The ordered data points.
     * @return The calculated trend direction.
     */
    private TrendDirection calculateTrendDirection(List<TrendDataPoint> dataPoints) {
        if (dataPoints == null || dataPoints.size() < 2) {
            return TrendDirection.STABLE;
        }

        var first = dataPoints.getFirst().getRecordedValue();
        var last = dataPoints.getLast().getRecordedValue();

        if (first == null || last == null) return TrendDirection.STABLE;
        if (last > first) return TrendDirection.INCREASING;
        if (last < first) return TrendDirection.DECREASING;

        return TrendDirection.STABLE;
    }
}
