package com.iotech.qualitrack.platform.ca.domain.model.valueobjects;

import com.iotech.qualitrack.platform.ca.domain.model.aggregates.DeviationAlert;

import java.util.List;

/**
 * An alert with the actions its container monitor executed for the same variable during the incident (US86).
 *
 * @param alert       the alert
 * @param actuations  related actions in time order; empty for alerts of the environment
 */
public record DeviationAlertDetail(DeviationAlert alert, List<RelatedActuation> actuations) {
    public DeviationAlertDetail {
        actuations = List.copyOf(actuations);
    }
}
