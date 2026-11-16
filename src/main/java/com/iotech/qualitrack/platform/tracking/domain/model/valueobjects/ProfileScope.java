package com.iotech.qualitrack.platform.tracking.domain.model.valueobjects;

/**
 * What an environmental profile configures: the environment supervised by its environmental device, or one container
 * monitor.
 */
public enum ProfileScope {
    /** Profile of an environment, evaluated with the readings of its environmental device. */
    ENVIRONMENT,
    /** Profile of one container monitor, with its thresholds and actuation rules. */
    CONTAINER_MONITOR
}
