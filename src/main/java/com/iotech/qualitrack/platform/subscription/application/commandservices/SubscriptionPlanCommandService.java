package com.iotech.qualitrack.platform.subscription.application.commandservices;

import com.iotech.qualitrack.platform.shared.application.result.ApplicationError;
import com.iotech.qualitrack.platform.shared.application.result.Result;
import com.iotech.qualitrack.platform.subscription.domain.model.commands.SeedSubscriptionPlansCommand;

/**
 * Application command service contract for subscription plan operations.
 */
public interface SubscriptionPlanCommandService {

    /**
     * Creates the plans of the catalog that the database does not have yet.
     *
     * @return the number of plans created
     */
    Result<Integer, ApplicationError> handle(SeedSubscriptionPlansCommand command);
}
