package com.iotech.qualitrack.platform.ra.application.internal.outboundservices;

import java.time.Instant;
import java.util.List;

/**
 * Snapshot of the environmental records of a laboratory in a period (US95): indicators of the readings, alerts and
 * actions of each environment.
 */
public record ComplianceReportData(Long laboratoryId, String laboratory, String from, String to,
                                   List<EnvironmentSection> environments, String generatedBy, Instant generatedAt) {
    public ComplianceReportData { environments = List.copyOf(environments); }

    /** Records of one environment. */
    public record EnvironmentSection(Long id, String code, String name, List<Indicator> indicators,
                                     List<Alert> alerts, List<Action> actions) {
        public EnvironmentSection {
            indicators = List.copyOf(indicators);
            alerts = List.copyOf(alerts);
            actions = List.copyOf(actions);
        }
    }

    /** Readings of one device and variable: summary, time in range and deviations. */
    public record Indicator(Long deviceId, String device, String metric, String unit, int readings, Double average,
                            Double minimum, Double maximum, Double timeInRangePercent, int deviations,
                            int criticalDeviations) {}

    /** Alert whose incident started in the period, with its current status. */
    public record Alert(Long id, Long deviceId, String device, String origin, String parameter, Double value,
                        Double threshold, String unit, String detectedAt, String severity, String status,
                        Integer deviationCount, String resolution) {}

    /** Action executed by a container monitor in the period. */
    public record Action(Long id, Long deviceId, String device, String action, String triggerMetric,
                         String triggerState, String result, String occurredAt) {}

    /** Readings of every environment. */
    public int readings() { return environments.stream().flatMap(item -> item.indicators().stream()).mapToInt(Indicator::readings).sum(); }

    /** Deviations of every environment. */
    public int deviations() { return environments.stream().flatMap(item -> item.indicators().stream()).mapToInt(Indicator::deviations).sum(); }

    /** Alerts of every environment. */
    public int alerts() { return environments.stream().mapToInt(item -> item.alerts().size()).sum(); }

    /** Actions of every environment. */
    public int actions() { return environments.stream().mapToInt(item -> item.actions().size()).sum(); }
}
