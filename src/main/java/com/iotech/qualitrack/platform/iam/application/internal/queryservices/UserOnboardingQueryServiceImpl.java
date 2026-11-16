package com.iotech.qualitrack.platform.iam.application.internal.queryservices;

import com.iotech.qualitrack.platform.iam.application.internal.outboundservices.acl.IamExternalSubscriptionService;
import com.iotech.qualitrack.platform.iam.application.queryservices.UserOnboardingQueryService;
import com.iotech.qualitrack.platform.iam.domain.model.queries.GetUserOnboardingQuery;
import com.iotech.qualitrack.platform.iam.domain.model.valueobjects.UserOnboarding;
import com.iotech.qualitrack.platform.iam.domain.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserOnboardingQueryServiceImpl implements UserOnboardingQueryService {
    private final UserRepository users;
    private final IamExternalSubscriptionService subscriptions;

    public UserOnboardingQueryServiceImpl(UserRepository users, IamExternalSubscriptionService subscriptions) {
        this.users = users;
        this.subscriptions = subscriptions;
    }

    @Override
    @Transactional(readOnly = true)
    public UserOnboarding handle(GetUserOnboardingQuery query) {
        var user = users.findById(query.userId()).filter(value -> value.isActive())
                .orElseThrow(() -> new IllegalArgumentException("Active user not found"));
        // The laboratory is the one Laboratory Management associated with the account when it created it.
        var laboratoryId = user.getLaboratoryId();
        var subscription = subscriptions.findSubscription(user.getId(), laboratoryId);
        return new UserOnboarding(user.getId(), laboratoryId, subscription.subscriptionId(),
                subscription.active() ? "ACTIVE" : "INACTIVE", user.isPasswordChangeRequired());
    }
}
