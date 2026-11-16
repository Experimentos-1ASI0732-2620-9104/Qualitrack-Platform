package com.iotech.qualitrack.platform.laboratory.application.commandservices;

import com.iotech.qualitrack.platform.laboratory.domain.model.commands.CreateLaboratoryCommand;
import com.iotech.qualitrack.platform.shared.application.result.ApplicationError;
import com.iotech.qualitrack.platform.shared.application.result.Result;

public interface LaboratoryOnboardingService {
    /**
     * Creates the laboratory of the user and associates it with the account and the active subscription.
     *
     * @return the laboratory, or a conflict when the account already has one and a business rule violation when the
     * user has no active subscription
     */
    Result<Long, ApplicationError> create(Long userId, CreateLaboratoryCommand command);
}
