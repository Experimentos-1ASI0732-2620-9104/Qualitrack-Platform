package com.iotech.qualitrack.platform.ca.interfaces.acl;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

/**
 * ACL facade exposed by the Compliance and Alerts bounded context.
 *
 * <p>Provides a stable interface for other bounded contexts to interact with CA
 * without depending on CA internal application services or domain commands.</p>
 */
public interface ComplianceContextFacade {

    /**
     * Alerts of an environment and its containers whose incident started in a period, oldest first (US95).
     *
     * @param laboratoryId the laboratory of the environment
     * @param environmentId the environment
     * @param from start of the period, inclusive
     * @param to end of the period, inclusive
     * @return the alerts with their current status
     */
    List<AlertReference> findEnvironmentAlerts(Long laboratoryId, Long environmentId, Instant from, Instant to);

    /**
     * Alerts recorded for a product batch, in the order they were recorded (US96).
     *
     * @param batchId the product batch
     * @return the alerts with their current status
     */
    List<AlertReference> findBatchAlerts(Long batchId);

    /**
     * Alerts detected by any of the given devices, used by the operational counts of the indicators (US93).
     *
     * @param deviceIds the equipment identifiers
     * @return the alerts with their current status, grouped by device
     */
    List<AlertReference> findDeviceAlerts(Collection<Long> deviceIds);

    /**
     * Deviation alert shared with other bounded contexts.
     *
     * @param id the alert
     * @param deviceId the environmental device or container monitor that detected it
     * @param origin ENVIRONMENT or CONTAINER
     * @param parameterName the monitored variable
     * @param recordedValue the value of the deviation that set the current severity
     * @param thresholdValue the limit crossed by that value
     * @param unit the unit of the value
     * @param detectedAt when the incident started
     * @param severity the highest severity of the incident
     * @param status the current status
     * @param deviationCount deviations grouped in the incident
     * @param resolutionNotes notes of the resolution, if resolved
     * @param recordedAt when the incident started as it was stored, also for older alerts whose time is not an
     *                   ISO-8601 instant
     */
    /**
     * Deviation alert shared with other bounded contexts.
     *
     * @param id the alert
     * @param deviceId the environmental device or container monitor that detected it
     * @param origin ENVIRONMENT or CONTAINER
     * @param parameterName the monitored variable
     * @param recordedValue the value of the deviation that set the current severity
     * @param thresholdValue the limit crossed by that value
     * @param unit the unit of the value
     * @param detectedAt when the incident started
     * @param severity the highest severity of the incident
     * @param status the current status
     * @param deviationCount deviations grouped in the incident
     * @param resolutionNotes notes of the resolution, if resolved
     * @param recordedAt when the incident started as it was stored, also for older alerts whose time is not an
     *                   ISO-8601 instant
     */
    public record AlertReference(Long id, Long deviceId, String origin, String parameterName, Double recordedValue,
                                 Double thresholdValue, String unit, Instant detectedAt, String severity,
                                 String status, Integer deviationCount, String resolutionNotes, String recordedAt) {
        /**
         * @return true while the incident is not resolved
         */
        public boolean isOpen() {
            return !"RESOLVED".equals(status);
        }
    }

    /**
     * Verifies whether a batch can be released according to compliance rules.
     *
     * <p>At this stage, the rule is intentionally conservative and simple:
     * a batch can be released when there are no unresolved non-compliance events
     * associated with that batch identifier.</p>
     *
     * @param batchId the batch identifier
     * @return true when the batch can be released
     */
    boolean canReleaseBatch(Long batchId);
}
