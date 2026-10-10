package com.iotech.qualitrack.platform.ca.application.commandservices;

import com.iotech.qualitrack.platform.ca.domain.model.commands.AcknowledgeAlertCommand;
import com.iotech.qualitrack.platform.ca.domain.model.aggregates.DeviationAlert;
import com.iotech.qualitrack.platform.ca.domain.model.commands.CreateDeviationAlertCommand;
import com.iotech.qualitrack.platform.ca.domain.model.commands.RecordConditionNormalizedCommand;
import com.iotech.qualitrack.platform.ca.domain.model.commands.ResolveAlertCommand;
import com.iotech.qualitrack.platform.ca.domain.model.commands.UpdateNotificationPreferenceCommand;
import com.iotech.qualitrack.platform.ca.domain.model.entities.NotificationPreference;
import com.iotech.qualitrack.platform.ca.domain.model.valueobjects.DeviationRegistration;
import com.iotech.qualitrack.platform.shared.application.result.ApplicationError;
import com.iotech.qualitrack.platform.shared.application.result.Result;

import java.util.Optional;

/**
 * Application service contract for commands over compliance alerts and notification preferences.
 */
public interface CaCommandService {

    /**
     * Registers a deviation of an environment or container: it opens a new alert, or is correlated with the open
     * alert of the same device and parameter (TS73).
     *
     * @param command command containing the deviation
     * @return the alert of the incident and whether it was created, or an application error
     * @see CreateDeviationAlertCommand
     */
    Result<DeviationRegistration, ApplicationError> handle(CreateDeviationAlertCommand command);

    /**
     * Notes in the open alert of a device and parameter that the condition returned to normal.
     *
     * @param command the return to normal
     * @return the updated alert, or empty when there is no open alert to update
     */
    Optional<DeviationAlert> handle(RecordConditionNormalizedCommand command);

    /**
     * Handles the acknowledgement of an existing deviation alert.
     *
     * @param command command containing alert and user acknowledgement data
     * @return acknowledged alert identifier (domain ID) or an application error
     * @see AcknowledgeAlertCommand
     */
    Result<Long, ApplicationError> handle(AcknowledgeAlertCommand command);

    /**
     * Handles the resolution of an existing deviation alert.
     *
     * @param command command containing alert resolution data
     * @return resolved alert identifier (domain ID) or an application error
     * @see ResolveAlertCommand
     */
    Result<Long, ApplicationError> handle(ResolveAlertCommand command);

    /**
     * Handles the update of user notification preferences.
     *
     * @param command command containing notification preference settings
     * @return updated notification preference or an application error
     * @see UpdateNotificationPreferenceCommand
     */
    Result<NotificationPreference, ApplicationError> handle(UpdateNotificationPreferenceCommand command);
}