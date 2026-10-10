package com.iotech.qualitrack.platform.iam.application.internal.queryservices;

import com.iotech.qualitrack.platform.iam.application.queryservices.UserOnboardingQueryService;
import com.iotech.qualitrack.platform.iam.domain.model.queries.GetUserOnboardingQuery;
import com.iotech.qualitrack.platform.iam.domain.model.valueobjects.UserOnboarding;
import com.iotech.qualitrack.platform.iam.domain.repositories.UserRepository;
import com.iotech.qualitrack.platform.laboratory.interfaces.acl.LaboratoryContextFacade;
import com.iotech.qualitrack.platform.subscription.interfaces.acl.SubscriptionContextFacade;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserOnboardingQueryServiceImpl implements UserOnboardingQueryService {
    private final UserRepository users;
    private final LaboratoryContextFacade laboratories;
    private final SubscriptionContextFacade subscriptions;

    public UserOnboardingQueryServiceImpl(UserRepository users, LaboratoryContextFacade laboratories,
                                         SubscriptionContextFacade subscriptions) {
        this.users = users;
        this.laboratories = laboratories;
        this.subscriptions = subscriptions;
    }

    @Override
    @Transactional(readOnly = true)
    public UserOnboarding handle(GetUserOnboardingQuery query) {
        var user = users.findById(query.userId()).filter(value -> value.isActive())
                .orElseThrow(() -> new IllegalArgumentException("Active user not found"));
        var laboratoryId = user.getLaboratoryId();
        if (laboratoryId != null && !laboratories.existsLaboratoryById(laboratoryId)) {
            throw new IllegalStateException("The user's laboratory association needs attention");
        }
        var access = subscriptions.getAccess(user.getId(), laboratoryId);
        return new UserOnboarding(user.getId(), laboratoryId, access.subscriptionId(),
                access.active() ? "ACTIVE" : "INACTIVE", user.isPasswordChangeRequired());
    }
}
