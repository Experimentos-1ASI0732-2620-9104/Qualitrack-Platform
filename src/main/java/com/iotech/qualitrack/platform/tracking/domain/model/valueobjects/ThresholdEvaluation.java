package com.iotech.qualitrack.platform.tracking.domain.model.valueobjects;

/**
 * Result of evaluating a value against an environmental threshold.
 *
 * @param state NORMAL, WARNING or CRITICAL
 * @param exceededLimit limit crossed by the value, or null when the state is NORMAL
 */
public record ThresholdEvaluation(EnvironmentalState state, Double exceededLimit) {
}
