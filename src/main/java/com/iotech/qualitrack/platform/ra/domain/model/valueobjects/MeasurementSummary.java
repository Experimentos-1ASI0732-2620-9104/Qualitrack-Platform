package com.iotech.qualitrack.platform.ra.domain.model.valueobjects;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

/**
 * Average, minimum and maximum of the numeric readings of one device, metric and unit in a period (US93, TS81).
 *
 * <p>Only persisted readings with a numeric value take part; a metric without readings has no summary instead of a
 * zero value.</p>
 *
 * @param environmentId   the environment of the device
 * @param deviceId        the environmental device or container monitor
 * @param metric          the measured metric
 * @param unit            the unit of the values
 * @param readings        number of readings summarized
 * @param average         arithmetic mean of the values
 * @param minimum         lowest value
 * @param maximum         highest value
 * @param firstMeasuredAt first reading of the period
 * @param lastMeasuredAt  last reading of the period
 */
public record MeasurementSummary(Long environmentId, Long deviceId, String metric, String unit, int readings,
                                 Double average, Double minimum, Double maximum, Instant firstMeasuredAt,
                                 Instant lastMeasuredAt) {

    /**
     * Summarizes the numeric readings of an environment per device, metric and unit.
     *
     * @param environmentId the environment of the readings
     * @param readings      readings of the period in any order
     * @return one summary per device, metric and unit with at least one numeric reading
     */
    public static List<MeasurementSummary> summarize(Long environmentId, List<EnvironmentalReading> readings) {
        var groups = new LinkedHashMap<List<Object>, List<EnvironmentalReading>>();
        readings.stream()
                .filter(EnvironmentalReading::isNumeric)
                .sorted(Comparator.comparing(EnvironmentalReading::measuredAt))
                .forEach(reading -> groups.computeIfAbsent(
                        List.of(reading.deviceId(), reading.metric(), Objects.toString(reading.unit(), "")),
                        key -> new ArrayList<>()).add(reading));
        return groups.values().stream().map(group -> {
            var first = group.getFirst();
            var statistics = group.stream().mapToDouble(EnvironmentalReading::value).summaryStatistics();
            return new MeasurementSummary(environmentId, first.deviceId(), first.metric(), first.unit(), group.size(),
                    round(statistics.getAverage()), statistics.getMin(), statistics.getMax(), first.measuredAt(),
                    group.getLast().measuredAt());
        }).sorted(Comparator.comparing(MeasurementSummary::deviceId).thenComparing(MeasurementSummary::metric)).toList();
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
