package com.iotech.qualitrack.platform.tracking.application.commandservices;

import com.iotech.qualitrack.platform.shared.application.result.ApplicationError;
import com.iotech.qualitrack.platform.shared.application.result.Result;
import com.iotech.qualitrack.platform.tracking.domain.model.aggregates.EnvironmentalProfile;
import com.iotech.qualitrack.platform.tracking.domain.model.commands.RecordActuationEventCommand;
import com.iotech.qualitrack.platform.tracking.domain.model.commands.RecordMeasurementCommand;
import com.iotech.qualitrack.platform.tracking.domain.model.commands.UpdateActuationRulesCommand;
import com.iotech.qualitrack.platform.tracking.domain.model.commands.UpdateContainerMonitorThresholdsCommand;
import com.iotech.qualitrack.platform.tracking.domain.model.commands.UpdateEnvironmentThresholdsCommand;
import com.iotech.qualitrack.platform.tracking.domain.model.entities.ActuationEvent;
import com.iotech.qualitrack.platform.tracking.domain.model.entities.Measurement;

/**
 * Use cases that change the state of Tracking &amp; Telemetry: environmental profiles, readings and actions of the
 * IoT devices.
 */
public interface TrackingCommandService {

    /**
     * Replaces the thresholds of an environment, creating its profile on the first change.
     *
     * @return the saved profile with its new version, or a validation error when the environment has no
     * environmental device or the thresholds are not valid
     */
    Result<EnvironmentalProfile, ApplicationError> handle(UpdateEnvironmentThresholdsCommand command);

    /**
     * Replaces the thresholds of a container monitor, creating its profile on the first change.
     *
     * @return the saved profile with its new version, or a validation error when the device is not a container
     * monitor of the environment or the thresholds are not valid
     */
    Result<EnvironmentalProfile, ApplicationError> handle(UpdateContainerMonitorThresholdsCommand command);

    /**
     * Replaces the actuation rules of a container monitor, creating its profile on the first change.
     *
     * @return the saved profile with its new version, or a validation error when the device is not a container
     * monitor of the environment or a rule is not valid
     */
    Result<EnvironmentalProfile, ApplicationError> handle(UpdateActuationRulesCommand command);

    /**
     * Records a reading, or returns the one already received for the same device, metric and moment.
     *
     * <p>A new reading is evaluated with the current profile and publishes the measurement recorded event and, when
     * the condition worsens or returns to NORMAL, the deviation or normalization event.</p>
     *
     * @return the stored reading, or a validation error when the device is not in the environment, does not report
     * the metric, the moment is in the future or the value does not match the metric
     */
    Result<Recorded<Measurement>, ApplicationError> handle(RecordMeasurementCommand command);

    /**
     * Records an action, or returns the one already received for the same device, action and moment.
     *
     * @return the stored action, or a validation error when the device is not a container monitor of the
     * environment, the cause is not valid or the moment is in the future
     */
    Result<Recorded<ActuationEvent>, ApplicationError> handle(RecordActuationEventCommand command);

    /**
     * Stored record and whether this request created it (false for a record re-sent by the Edge).
     *
     * @param value   the stored record
     * @param created true when this request stored it; false when it already existed
     * @param <T>     type of the record
     */
    record Recorded<T>(T value, boolean created) {
    }
}
