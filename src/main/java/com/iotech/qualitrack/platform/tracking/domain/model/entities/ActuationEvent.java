package com.iotech.qualitrack.platform.tracking.domain.model.entities;

import com.iotech.qualitrack.platform.tracking.domain.model.valueobjects.ActuationAction;
import com.iotech.qualitrack.platform.tracking.domain.model.valueobjects.ActuationResult;
import com.iotech.qualitrack.platform.tracking.domain.model.valueobjects.EnvironmentalState;
import com.iotech.qualitrack.platform.tracking.domain.model.valueobjects.MonitoredMetric;
import lombok.Getter;

import java.time.Instant;

/**
 * Physical action executed by a container monitor and its result (Actuation Event).
 *
 * <p>The device decides and executes the action locally with the last valid profile; the platform keeps the record
 * of what happened. It is different from an alert of Compliance &amp; Alerting.</p>
 */
@Getter
public class ActuationEvent {

    private Long id;

    private Long laboratoryId;

    private Long environmentId;

    private Long deviceId;

    private ActuationAction action;

    /**
     * Metric whose condition triggered the action; null when the device stops an action without a cause.
     */
    private MonitoredMetric triggerMetric;

    /**
     * State that triggered the action; NORMAL when the condition recovered.
     */
    private EnvironmentalState triggerState;

    private ActuationResult result;

    private Instant occurredAt;

    private Long profileVersion;

    private Instant receivedAt;

    public ActuationEvent(Long id, Long laboratoryId, Long environmentId, Long deviceId, ActuationAction action,
                          MonitoredMetric triggerMetric, EnvironmentalState triggerState, ActuationResult result,
                          Instant occurredAt, Long profileVersion, Instant receivedAt) {
        this.id = id;
        this.laboratoryId = laboratoryId;
        this.environmentId = environmentId;
        this.deviceId = deviceId;
        this.action = action;
        this.triggerMetric = triggerMetric;
        this.triggerState = triggerState;
        this.result = result;
        this.occurredAt = occurredAt;
        this.profileVersion = profileVersion;
        this.receivedAt = receivedAt;
    }

    /**
     * Records an action reported by a container monitor.
     *
     * @throws IllegalArgumentException when the action, the time or the cause are not valid
     */
    public static ActuationEvent record(Long laboratoryId, Long environmentId, Long deviceId, ActuationAction action,
                                       MonitoredMetric triggerMetric, EnvironmentalState triggerState,
                                       ActuationResult result, Instant occurredAt, Long profileVersion) {
        if (deviceId == null || deviceId <= 0) throw new IllegalArgumentException("The device is required");
        if (action == null) throw new IllegalArgumentException("The action is required");
        if (occurredAt == null) throw new IllegalArgumentException("The time of the action is required");
        if ((triggerMetric == null) != (triggerState == null)) {
            throw new IllegalArgumentException("The cause of an action needs both the metric and the state");
        }
        if (triggerMetric != null && !triggerMetric.hasThresholds()) {
            throw new IllegalArgumentException("Actions cannot be caused by " + triggerMetric);
        }
        return new ActuationEvent(null, laboratoryId, environmentId, deviceId, action, triggerMetric, triggerState,
                result == null ? ActuationResult.EXECUTED : result, occurredAt, profileVersion, null);
    }
}
