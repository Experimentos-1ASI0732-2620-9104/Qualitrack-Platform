package com.iotech.qualitrack.platform.laboratory.application.internal.commandservices;

import com.iotech.qualitrack.platform.laboratory.application.commandservices.LaboratoryCommandService;
import com.iotech.qualitrack.platform.laboratory.application.commandservices.LaboratoryOnboardingService;
import com.iotech.qualitrack.platform.laboratory.application.internal.outboundservices.acl.ExternalIamService;
import com.iotech.qualitrack.platform.laboratory.application.internal.outboundservices.acl.LaboratoryExternalSubscriptionService;
import com.iotech.qualitrack.platform.laboratory.domain.model.commands.CreateLaboratoryCommand;
import com.iotech.qualitrack.platform.shared.application.result.ApplicationError;
import com.iotech.qualitrack.platform.shared.application.result.ApplicationException;
import com.iotech.qualitrack.platform.shared.application.result.Result;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Creates and associates the laboratory atomically, serializing setup per user. */
@Service
public class LaboratoryOnboardingServiceImpl implements LaboratoryOnboardingService {
    private final LaboratoryCommandService commands;
    private final ExternalIamService users;
    private final LaboratoryExternalSubscriptionService subscriptions;

    public LaboratoryOnboardingServiceImpl(LaboratoryCommandService commands, ExternalIamService users,
                                       LaboratoryExternalSubscriptionService subscriptions) {
        this.commands = commands;
        this.users = users;
        this.subscriptions = subscriptions;
    }

    @Transactional
    @Override
    public Result<Long, ApplicationError> create(Long userId, CreateLaboratoryCommand command) {
        if (users.lockLaboratoryAssociation(userId)) {
            return Result.failure(ApplicationError.conflict("Laboratory", "Laboratory setup is already complete"));
        }
        if (!subscriptions.hasActiveSubscription(userId)) {
            return Result.failure(ApplicationError.businessRuleViolation("subscription", "An active subscription is required"));
        }
        var laboratoryId = switch (commands.handle(command)) {
            case Result.Success<Long, ApplicationError> success -> success.value();
            // Raised so that a laboratory saved before the failure is rolled back with the rest of the setup.
            case Result.Failure<Long, ApplicationError> failure -> throw new ApplicationException(failure.error());
        };
        users.assignLaboratory(userId, laboratoryId);
        subscriptions.assignLaboratory(userId, laboratoryId);
        return Result.success(laboratoryId);
    }
}
