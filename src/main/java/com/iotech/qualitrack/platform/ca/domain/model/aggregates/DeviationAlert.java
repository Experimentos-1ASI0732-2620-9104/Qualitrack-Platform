package com.iotech.qualitrack.platform.ca.domain.model.aggregates;

import com.iotech.qualitrack.platform.ca.domain.model.commands.AcknowledgeAlertCommand;
import com.iotech.qualitrack.platform.ca.domain.model.commands.CreateDeviationAlertCommand;
import com.iotech.qualitrack.platform.ca.domain.model.commands.ResolveAlertCommand;
import com.iotech.qualitrack.platform.ca.domain.model.valueobjects.AlertOrigin;
import com.iotech.qualitrack.platform.ca.domain.model.valueobjects.AlertSeverity;
import com.iotech.qualitrack.platform.ca.domain.model.valueobjects.AlertStatus;
import com.iotech.qualitrack.platform.shared.domain.model.aggregates.AbstractDomainAggregateRoot;
import lombok.Getter;

import java.time.Instant;
import java.util.Objects;

/**
 * The DeviationAlert Aggregate Root.
 *
 * <p>Represents an incident that requires attention: a deviation of an environment or of a monitored container,
 * detected by its IoT device. It governs the lifecycle of the alert from creation to acknowledgement and final
 * resolution. While the alert is open, new deviations of the same device and parameter belong to the same incident:
 * they are counted, the severity rises when the condition gets worse and a return to normal is noted, without
 * creating new alerts (decisions of 2026-10-04).</p>
 */
@Getter
public class DeviationAlert extends AbstractDomainAggregateRoot<DeviationAlert> {

    /**
     * The unique numeric identifier for the deviation alert.
     */
    private Long id;

    /**
     * The laboratory of the alert; null only for alerts registered before environments existed.
     */
    private Long laboratoryId;

    /**
     * The environment where the deviation was detected; null only for alerts registered before environments existed.
     */
    private Long environmentId;

    /**
     * Whether the alert originated in the environment or in a monitored container; null for older alerts.
     */
    private AlertOrigin origin;

    /**
     * The numeric identifier of the equipment or IoT device that generated the alert.
     */
    private Long equipmentId;

    /**
     * The numeric identifier of the production batch associated with the alert, if applicable.
     */
    private Long batchId;

    /**
     * The Tracking measurement that opened the incident, when the alert comes from a reading.
     */
    private Long measurementId;

    /**
     * The Tracking measurement of the latest deviation of the incident.
     */
    private Long lastMeasurementId;

    /**
     * The name of the monitored process parameter.
     */
    private String parameterName;

    /**
     * The measured value of the deviation that set the current severity.
     */
    private Double recordedValue;

    /**
     * The limit crossed by the deviation that set the current severity.
     */
    private Double thresholdValue;

    /**
     * The measurement unit for the recorded and threshold values.
     */
    private String unit;

    /**
     * The timestamp when the incident started.
     */
    private String timestamp;

    /**
     * The highest severity reached by the incident.
     */
    private AlertSeverity severity;

    /**
     * The current lifecycle status of the alert.
     */
    private AlertStatus status;

    /**
     * Number of deviations correlated with the incident, including the one that opened it.
     */
    private Integer deviationCount;

    /**
     * When the latest deviation of the incident was measured.
     */
    private Instant lastDetectedAt;

    /**
     * When the condition returned to normal after the latest deviation; null while it is still deviated.
     */
    private Instant normalizedAt;

    /**
     * The numeric identifier of the user who acknowledged the alert, if applicable.
     */
    private Long acknowledgedBy;

    /**
     * When the alert was acknowledged, if applicable.
     */
    private Instant acknowledgedAt;

    /**
     * The numeric identifier of the user who resolved the alert, if applicable.
     */
    private Long resolvedBy;

    /**
     * When the alert was resolved, if applicable.
     */
    private Instant resolvedAt;

    /**
     * Notes describing the corrective action or resolution, if applicable.
     */
    private String resolutionNotes;

    /**
     * Default constructor.
     * Required by the persistence and mapping layers to reconstruct the entity.
     */
    public DeviationAlert() {
        // Required for reconstruction by JPA or Assemblers
    }

    /**
     * Reconstructs an alert registered for an equipment, without environment nor incident data.
     *
     * @param id The numeric alert ID.
     * @param equipmentId The equipment ID.
     * @param batchId The batch ID, if applicable.
     * @param parameterName The monitored parameter name.
     * @param recordedValue The measured value.
     * @param thresholdValue The threshold value.
     * @param unit The measurement unit.
     * @param timestamp The deviation timestamp.
     * @param severity The alert severity.
     * @param status The alert status.
     * @param acknowledgedBy The user who acknowledged the alert, if applicable.
     * @param resolvedBy The user who resolved the alert, if applicable.
     * @param resolutionNotes Resolution or corrective action notes, if applicable.
     */
    public DeviationAlert(
            Long id,
            Long equipmentId,
            Long batchId,
            String parameterName,
            Double recordedValue,
            Double thresholdValue,
            String unit,
            String timestamp,
            AlertSeverity severity,
            AlertStatus status,
            Long acknowledgedBy,
            Long resolvedBy,
            String resolutionNotes
    ) {
        this(id, null, null, null, equipmentId, batchId, null, null, parameterName, recordedValue, thresholdValue,
                unit, timestamp, severity, status, 1, null, null, acknowledgedBy, null, resolvedBy, null, resolutionNotes);
    }

    /**
     * Reconstructs a DeviationAlert from persistence data.
     */
    public DeviationAlert(
            Long id,
            Long laboratoryId,
            Long environmentId,
            AlertOrigin origin,
            Long equipmentId,
            Long batchId,
            Long measurementId,
            Long lastMeasurementId,
            String parameterName,
            Double recordedValue,
            Double thresholdValue,
            String unit,
            String timestamp,
            AlertSeverity severity,
            AlertStatus status,
            Integer deviationCount,
            Instant lastDetectedAt,
            Instant normalizedAt,
            Long acknowledgedBy,
            Instant acknowledgedAt,
            Long resolvedBy,
            Instant resolvedAt,
            String resolutionNotes
    ) {
        this.id = id;
        this.laboratoryId = laboratoryId;
        this.environmentId = environmentId;
        this.origin = origin;
        this.equipmentId = equipmentId;
        this.batchId = batchId;
        this.measurementId = measurementId;
        this.lastMeasurementId = lastMeasurementId;
        this.parameterName = parameterName;
        this.recordedValue = recordedValue;
        this.thresholdValue = thresholdValue;
        this.unit = unit;
        this.timestamp = timestamp;
        this.severity = severity;
        this.status = status;
        this.deviationCount = deviationCount == null || deviationCount < 1 ? 1 : deviationCount;
        this.lastDetectedAt = lastDetectedAt;
        this.normalizedAt = normalizedAt;
        this.acknowledgedBy = acknowledgedBy;
        this.acknowledgedAt = acknowledgedAt;
        this.resolvedBy = resolvedBy;
        this.resolvedAt = resolvedAt;
        this.resolutionNotes = resolutionNotes;
    }

    /**
     * Opens the alert of a new incident.
     *
     * @param command The deviation that opens the incident.
     * @param deviceId The environmental device or container monitor that detected it.
     * @param origin Whether the device supervises the environment or a container.
     */
    public DeviationAlert(CreateDeviationAlertCommand command, Long deviceId, AlertOrigin origin) {
        Objects.requireNonNull(command, "Create deviation alert command is required");

        this.laboratoryId = command.laboratoryId();
        this.environmentId = command.environmentId();
        this.origin = Objects.requireNonNull(origin, "Origin is required");
        this.equipmentId = Objects.requireNonNull(deviceId, "Device ID is required");
        this.measurementId = command.measurementId();
        this.lastMeasurementId = command.measurementId();
        this.parameterName = command.parameterName();
        this.recordedValue = command.recordedValue();
        this.thresholdValue = command.thresholdValue();
        this.unit = command.unit();
        this.timestamp = command.detectedAt().toString();
        this.severity = command.severity();
        this.status = AlertStatus.UNRESOLVED;
        this.deviationCount = 1;
        this.lastDetectedAt = command.detectedAt();
    }

    /**
     * Adds a new deviation of the same device and parameter to this open incident. The severity only rises; the
     * value and limit are those of the deviation that set the current severity.
     *
     * @param command The new deviation.
     * @return true when the deviation raised the severity of the incident
     */
    public boolean registerDeviation(CreateDeviationAlertCommand command) {
        Objects.requireNonNull(command, "Create deviation alert command is required");
        if (!isOpen()) {
            throw new IllegalStateException("Resolved alerts do not receive new deviations");
        }
        this.deviationCount = this.deviationCount + 1;
        if (lastDetectedAt == null || command.detectedAt().isAfter(lastDetectedAt)) {
            this.lastDetectedAt = command.detectedAt();
            this.lastMeasurementId = command.measurementId();
        }
        if (normalizedAt != null && !command.detectedAt().isBefore(normalizedAt)) {
            this.normalizedAt = null;
        }
        if (command.severity().ordinal() < this.severity.ordinal()) {
            return false;
        }
        var escalated = command.severity().ordinal() > this.severity.ordinal();
        if (escalated) {
            this.severity = command.severity();
            this.recordedValue = command.recordedValue();
            this.thresholdValue = command.thresholdValue();
        }
        return escalated;
    }

    /**
     * Notes that the condition returned to normal after the latest deviation. The alert stays open: only a person
     * closes the incident with its resolution.
     *
     * @param at When the normal value was measured.
     * @return true when the note was registered
     */
    public boolean markNormalized(Instant at) {
        Objects.requireNonNull(at, "Normalization time is required");
        if (!isOpen() || normalizedAt != null || (lastDetectedAt != null && !at.isAfter(lastDetectedAt))) {
            return false;
        }
        this.normalizedAt = at;
        return true;
    }

    /**
     * Acknowledges the deviation alert.
     *
     * @param command The command containing acknowledgement information.
     * @param at When the user started attending the alert.
     */
    public void acknowledge(AcknowledgeAlertCommand command, Instant at) {
        Objects.requireNonNull(command, "Acknowledge alert command is required");

        if (this.status == AlertStatus.RESOLVED) {
            throw new IllegalStateException("Resolved alerts cannot be acknowledged");
        }
        if (this.status == AlertStatus.ACKNOWLEDGED) {
            throw new IllegalStateException("Alert is already acknowledged");
        }

        this.status = AlertStatus.ACKNOWLEDGED;
        this.acknowledgedBy = Objects.requireNonNull(command.acknowledgedBy(), "Acknowledged by is required");
        this.acknowledgedAt = Objects.requireNonNull(at, "Acknowledgement time is required");
    }

    /**
     * Resolves the deviation alert after corrective action.
     *
     * @param command The command containing resolution information.
     * @param at When the alert was resolved.
     */
    public void resolve(ResolveAlertCommand command, Instant at) {
        Objects.requireNonNull(command, "Resolve alert command is required");

        if (this.status == AlertStatus.RESOLVED) {
            throw new IllegalStateException("Alert is already resolved");
        }

        this.status = AlertStatus.RESOLVED;
        this.resolvedBy = Objects.requireNonNull(command.resolvedBy(), "Resolved by is required");
        this.resolvedAt = Objects.requireNonNull(at, "Resolution time is required");
        this.resolutionNotes = Objects.requireNonNull(command.resolutionNotes(), "Resolution notes are required");
    }

    /**
     * Indicates whether this alert is still unresolved.
     *
     * @return true if the alert is unresolved; otherwise false.
     */
    public boolean isUnresolved() {
        return this.status == AlertStatus.UNRESOLVED;
    }

    /**
     * Indicates whether the incident is still open (unresolved or being attended).
     *
     * @return true if the alert is not resolved yet; otherwise false.
     */
    public boolean isOpen() {
        return this.status != AlertStatus.RESOLVED;
    }

    /**
     * Indicates whether this alert is critical.
     *
     * @return true if the alert severity is critical; otherwise false.
     */
    public boolean isCritical() {
        return this.severity == AlertSeverity.CRITICAL;
    }

    /**
     * When the incident started, or null when the stored timestamp is not an ISO-8601 instant (older alerts).
     *
     * @return the detection instant
     */
    public Instant detectedAt() {
        try {
            return timestamp == null ? null : Instant.parse(timestamp);
        } catch (RuntimeException exception) {
            return null;
        }
    }
}
