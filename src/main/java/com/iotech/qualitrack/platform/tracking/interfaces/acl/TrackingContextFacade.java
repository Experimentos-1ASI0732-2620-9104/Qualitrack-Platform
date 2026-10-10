package com.iotech.qualitrack.platform.tracking.interfaces.acl;

import java.time.Instant;
import java.util.List;

/**
 * Anti-corruption facade that exposes Tracking &amp; Telemetry records to other bounded contexts.
 */
public interface TrackingContextFacade {

    /**
     * Actions executed by a container monitor for a metric in a period, oldest first (US86).
     *
     * @param laboratoryId the laboratory that owns the device
     * @param deviceId the container monitor
     * @param metric the metric that triggered the actions
     * @param from start of the period, inclusive
     * @param to end of the period, inclusive
     * @return the actions; empty when the device executed none
     */
    List<ActuationReference> findActuations(Long laboratoryId, Long deviceId, String metric, Instant from, Instant to);

    /**
     * Actions executed by the container monitors of an environment in a period, oldest first (US95).
     *
     * @param laboratoryId the laboratory that owns the environment
     * @param environmentId the environment
     * @param from start of the period, inclusive
     * @param to end of the period, inclusive
     * @return the actions; empty when the devices executed none
     */
    List<ActuationReference> findEnvironmentActuations(Long laboratoryId, Long environmentId, Instant from, Instant to);

    /**
     * Readings of the devices of an environment in a period, oldest first (US93-US95).
     *
     * @param laboratoryId the laboratory that owns the environment
     * @param environmentId the environment
     * @param from start of the period, inclusive
     * @param to end of the period, inclusive
     * @return the readings; empty when the devices sent none
     */
    List<MeasurementReference> findMeasurements(Long laboratoryId, Long environmentId, Instant from, Instant to);

    /**
     * Action executed by a device, shared with other bounded contexts.
     *
     * @param id the actuation event
     * @param deviceId the container monitor that executed it
     * @param action the executed action
     * @param triggerMetric the metric that triggered it, when reported
     * @param triggerState the condition that triggered it, when reported
     * @param result EXECUTED or FAILED
     * @param occurredAt when the device executed it
     */
    record ActuationReference(Long id, Long deviceId, String action, String triggerMetric, String triggerState,
                              String result, Instant occurredAt) {
    }

    /**
     * Reading of a device, shared with other bounded contexts.
     *
     * @param id the measurement
     * @param deviceId the environmental device or container monitor that sent it
     * @param metric the measured metric
     * @param value the numeric value, or null for readings without one (RFID tag)
     * @param unit the unit of the value
     * @param state NORMAL, WARNING or CRITICAL as evaluated by the platform, or null when it was not evaluated
     * @param measuredAt when the device measured it
     */
    record MeasurementReference(Long id, Long deviceId, String metric, Double value, String unit, String state,
                                Instant measuredAt) {
    }
}
