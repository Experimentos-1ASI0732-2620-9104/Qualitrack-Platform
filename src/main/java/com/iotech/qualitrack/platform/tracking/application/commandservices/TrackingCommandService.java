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

    Result<EnvironmentalProfile, ApplicationError> handle(UpdateEnvironmentThresholdsCommand command);

    Result<EnvironmentalProfile, ApplicationError> handle(UpdateContainerMonitorThresholdsCommand command);

    Result<EnvironmentalProfile, ApplicationError> handle(UpdateActuationRulesCommand command);

    /**
     * Records a reading, or returns the one already received for the same device, metric and moment.
     */
    Result<Recorded<Measurement>, ApplicationError> handle(RecordMeasurementCommand command);

    /**
     * Records an action, or returns the one already received for the same device, action and moment.
     */
    Result<Recorded<ActuationEvent>, ApplicationError> handle(RecordActuationEventCommand command);

    /**
     * Stored record and whether this request created it (false for a record re-sent by the Edge).
     */
    record Recorded<T>(T value, boolean created) {
    }
}
