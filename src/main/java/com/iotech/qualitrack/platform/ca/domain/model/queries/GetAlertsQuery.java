package com.iotech.qualitrack.platform.ca.domain.model.queries;

import com.iotech.qualitrack.platform.ca.domain.model.valueobjects.AlertSeverity;
import com.iotech.qualitrack.platform.ca.domain.model.valueobjects.AlertStatus;

/**
 * Query for the alerts of an environment and its monitored containers (US85, TS74).
 *
 * @param laboratoryId  the laboratory of the environment
 * @param environmentId the environment
 * @param deviceId      optional environmental device or container monitor
 * @param status        optional lifecycle status
 * @param severity      optional severity
 * @param activeOnly    true to keep only open alerts (unresolved or being attended)
 */
public record GetAlertsQuery(
        Long laboratoryId,
        Long environmentId,
        Long deviceId,
        AlertStatus status,
        AlertSeverity severity,
        boolean activeOnly
) {
    /**
     * Compact constructor for GetAlertsQuery.
     * Enforces Fail-Fast validation for provided filters.
     */
    public GetAlertsQuery {
        if (laboratoryId == null || laboratoryId <= 0) {
            throw new IllegalArgumentException("laboratoryId must be greater than 0.");
        }
        if (environmentId == null || environmentId <= 0) {
            throw new IllegalArgumentException("environmentId must be greater than 0.");
        }
        if (deviceId != null && deviceId <= 0) {
            throw new IllegalArgumentException("deviceId must be greater than 0.");
        }
    }
}
