package com.iotech.qualitrack.platform.tracking.domain.model.valueobjects;

/**
 * Relates a condition of the container with the automatic action the container monitor executes (Actuation Rule).
 *
 * @param metric measured quantity of the condition
 * @param state  WARNING or CRITICAL condition that triggers the action
 * @param action activation executed by the device
 */
public record ActuationRule(MonitoredMetric metric, EnvironmentalState state, ActuationAction action) {

    public ActuationRule {
        if (metric == null || state == null || action == null) {
            throw new IllegalArgumentException("A rule needs a metric, a state and an action");
        }
        if (!metric.hasThresholds()) throw new IllegalArgumentException("Rules cannot be based on " + metric);
        if (state == EnvironmentalState.NORMAL) {
            throw new IllegalArgumentException("A rule reacts to a WARNING or CRITICAL condition");
        }
        if (!action.isActivation()) {
            throw new IllegalArgumentException("A rule must start an action; " + action + " stops it");
        }
    }
}
