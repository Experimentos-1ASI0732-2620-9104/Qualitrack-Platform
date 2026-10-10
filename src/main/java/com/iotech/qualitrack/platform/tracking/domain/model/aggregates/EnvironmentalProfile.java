package com.iotech.qualitrack.platform.tracking.domain.model.aggregates;

import com.iotech.qualitrack.platform.shared.domain.model.aggregates.AbstractDomainAggregateRoot;
import com.iotech.qualitrack.platform.tracking.domain.model.valueobjects.ActuationRule;
import com.iotech.qualitrack.platform.tracking.domain.model.valueobjects.EnvironmentalThreshold;
import com.iotech.qualitrack.platform.tracking.domain.model.valueobjects.MonitoredMetric;
import com.iotech.qualitrack.platform.tracking.domain.model.valueobjects.ProfileScope;
import com.iotech.qualitrack.platform.tracking.domain.model.valueobjects.ThresholdEvaluation;
import lombok.Getter;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * The Environmental Profile Aggregate Root.
 *
 * <p>Current configuration used to evaluate the conditions of an environment (through its environmental device) or
 * of a container monitor: the WARNING/CRITICAL thresholds of each metric and, for a container monitor, the actuation
 * rules that relate a condition with an automatic action. Every change increases the version, so the devices and the
 * Edge know which configuration they apply and each measurement records the version used to evaluate it.</p>
 */
@Getter
public class EnvironmentalProfile extends AbstractDomainAggregateRoot<EnvironmentalProfile> {

    private Long id;

    private Long laboratoryId;

    private ProfileScope scope;

    /**
     * Environment configured by the profile; set for the ENVIRONMENT scope.
     */
    private Long environmentId;

    /**
     * Container monitor configured by the profile; set for the CONTAINER_MONITOR scope.
     */
    private Long deviceId;

    /**
     * Configuration version, 0 until the first change.
     */
    private long version;

    private List<EnvironmentalThreshold> thresholds;

    private List<ActuationRule> actuationRules;

    /**
     * User that made the last change.
     */
    private Long updatedBy;

    private Instant updatedAt;

    /**
     * Reconstructs a profile from persistence data.
     */
    public EnvironmentalProfile(Long id, Long laboratoryId, ProfileScope scope, Long environmentId, Long deviceId,
                                long version, List<EnvironmentalThreshold> thresholds, List<ActuationRule> actuationRules,
                                Long updatedBy, Instant updatedAt) {
        this.id = id;
        this.laboratoryId = laboratoryId;
        this.scope = scope;
        this.environmentId = environmentId;
        this.deviceId = deviceId;
        this.version = version;
        this.thresholds = List.copyOf(thresholds);
        this.actuationRules = List.copyOf(actuationRules);
        this.updatedBy = updatedBy;
        this.updatedAt = updatedAt;
    }

    /**
     * Starts the empty profile of an environment.
     */
    public static EnvironmentalProfile forEnvironment(Long laboratoryId, Long environmentId) {
        requirePositive(laboratoryId, "Laboratory ID");
        requirePositive(environmentId, "Environment ID");
        return new EnvironmentalProfile(null, laboratoryId, ProfileScope.ENVIRONMENT, environmentId, null, 0,
                List.of(), List.of(), null, null);
    }

    /**
     * Starts the empty profile of a container monitor.
     */
    public static EnvironmentalProfile forContainerMonitor(Long laboratoryId, Long deviceId) {
        requirePositive(laboratoryId, "Laboratory ID");
        requirePositive(deviceId, "Device ID");
        return new EnvironmentalProfile(null, laboratoryId, ProfileScope.CONTAINER_MONITOR, null, deviceId, 0,
                List.of(), List.of(), null, null);
    }

    /**
     * Replaces every threshold of the profile (US56, US57, US58).
     *
     * @throws IllegalArgumentException when a metric is not reported by the configured device, is repeated, or loses
     *                                  the threshold an actuation rule depends on
     */
    public void replaceThresholds(List<EnvironmentalThreshold> newThresholds, Long userId, Instant now) {
        if (newThresholds == null) throw new IllegalArgumentException("The thresholds are required");
        var metrics = new HashSet<MonitoredMetric>();
        for (var threshold : newThresholds) {
            if (!threshold.metric().isReportedBy(deviceKind())) {
                throw new IllegalArgumentException(threshold.metric() + " is not measured by the "
                        + (scope == ProfileScope.ENVIRONMENT ? "environmental device" : "container monitor"));
            }
            if (!metrics.add(threshold.metric())) {
                throw new IllegalArgumentException("Only one threshold per metric is allowed: " + threshold.metric());
            }
        }
        requireRulesCovered(actuationRules, metrics);
        this.thresholds = List.copyOf(newThresholds);
        touch(userId, now);
    }

    /**
     * Replaces the actuation rules of a container monitor (US59).
     *
     * @throws IllegalArgumentException when the profile is not of a container monitor, a rule is repeated or its
     *                                  metric has no threshold to evaluate the condition
     */
    public void replaceActuationRules(List<ActuationRule> rules, Long userId, Instant now) {
        if (scope != ProfileScope.CONTAINER_MONITOR) {
            throw new IllegalArgumentException("Only container monitors execute automatic actions");
        }
        if (rules == null) throw new IllegalArgumentException("The actuation rules are required");
        if (new HashSet<>(rules).size() != rules.size()) throw new IllegalArgumentException("The rules cannot be repeated");
        requireRulesCovered(rules, configuredMetrics());
        this.actuationRules = List.copyOf(rules);
        touch(userId, now);
    }

    /**
     * Evaluates a value of a metric; empty when the metric has no threshold in this profile.
     */
    public Optional<ThresholdEvaluation> evaluate(MonitoredMetric metric, double value) {
        return thresholds.stream().filter(threshold -> threshold.metric() == metric).findFirst()
                .map(threshold -> threshold.evaluate(value));
    }

    private Set<MonitoredMetric> configuredMetrics() {
        var metrics = new HashSet<MonitoredMetric>();
        thresholds.forEach(threshold -> metrics.add(threshold.metric()));
        return metrics;
    }

    private void requireRulesCovered(List<ActuationRule> rules, Set<MonitoredMetric> metrics) {
        for (var rule : rules) {
            if (!metrics.contains(rule.metric())) {
                throw new IllegalArgumentException("A rule on " + rule.metric() + " needs a threshold for that metric");
            }
        }
    }

    private String deviceKind() {
        return (scope == ProfileScope.ENVIRONMENT ? MonitoredMetric.DeviceKind.ENVIRONMENTAL_DEVICE
                : MonitoredMetric.DeviceKind.CONTAINER_MONITOR).name();
    }

    private void touch(Long userId, Instant now) {
        this.version++;
        this.updatedBy = userId;
        this.updatedAt = now;
    }

    private static void requirePositive(Long value, String name) {
        if (value == null || value <= 0) throw new IllegalArgumentException(name + " must be a positive number");
    }
}
