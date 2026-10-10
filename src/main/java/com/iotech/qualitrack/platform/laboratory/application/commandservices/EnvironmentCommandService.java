package com.iotech.qualitrack.platform.laboratory.application.commandservices;

import com.iotech.qualitrack.platform.laboratory.domain.model.aggregates.Environment;
import com.iotech.qualitrack.platform.laboratory.domain.model.commands.AssignEnvironmentUsageCommand;
import com.iotech.qualitrack.platform.laboratory.domain.model.commands.RegisterEnvironmentCommand;
import com.iotech.qualitrack.platform.laboratory.domain.model.commands.UpdateEnvironmentCommand;
import com.iotech.qualitrack.platform.shared.application.result.ApplicationError;
import com.iotech.qualitrack.platform.shared.application.result.Result;

/**
 * Application service contract for commands over laboratory environments.
 */
public interface EnvironmentCommandService {

    /**
     * Handles the registration of a new environment in a laboratory.
     *
     * @param command command containing the laboratory and environment data
     * @return the registered environment or an application error
     */
    Result<Environment, ApplicationError> handle(RegisterEnvironmentCommand command);

    /**
     * Handles the update of the identification data of an environment.
     *
     * @param command command containing the target environment and its new data
     * @return the updated environment or an application error
     */
    Result<Environment, ApplicationError> handle(UpdateEnvironmentCommand command);

    /**
     * Handles the assignment of the main use of an environment.
     *
     * @param command command containing the target environment and the usage
     * @return the environment with its new usage or an application error
     */
    Result<Environment, ApplicationError> handle(AssignEnvironmentUsageCommand command);
}
