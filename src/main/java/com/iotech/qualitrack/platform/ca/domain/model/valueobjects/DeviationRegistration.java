package com.iotech.qualitrack.platform.ca.domain.model.valueobjects;

import com.iotech.qualitrack.platform.ca.domain.model.aggregates.DeviationAlert;

/**
 * Result of registering a deviation: the alert of the incident and whether the deviation opened it or was correlated
 * with an alert that was already open.
 *
 * @param alert   the alert of the incident
 * @param created true when the deviation opened a new alert
 */
public record DeviationRegistration(DeviationAlert alert, boolean created) {
}
