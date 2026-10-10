package com.iotech.qualitrack.platform.ca.domain.model.queries;

/**
 * Query for an alert with its origin and the actions related to the incident (US86).
 *
 * @param alertId the alert
 */
public record GetAlertDetailQuery(Long alertId) {
    public GetAlertDetailQuery {
        if (alertId == null || alertId <= 0) {
            throw new IllegalArgumentException("alertId must be greater than 0.");
        }
    }
}
