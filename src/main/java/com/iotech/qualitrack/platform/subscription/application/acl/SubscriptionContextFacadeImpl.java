package com.iotech.qualitrack.platform.subscription.application.acl;

import com.iotech.qualitrack.platform.subscription.application.queryservices.SubscriptionQueryService;
import com.iotech.qualitrack.platform.subscription.domain.model.queries.GetActiveSubscriptionByLaboratoryIdQuery;
import com.iotech.qualitrack.platform.subscription.interfaces.acl.SubscriptionAccess;
import com.iotech.qualitrack.platform.subscription.interfaces.acl.SubscriptionContextFacade;
import org.springframework.stereotype.Service;

/**
 * Application-layer implementation of the {@link SubscriptionContextFacade} ACL facade.
 */
@Service
public class SubscriptionContextFacadeImpl implements SubscriptionContextFacade {

    private final SubscriptionQueryService subscriptionQueryService;
    private final com.iotech.qualitrack.platform.subscription.domain.repositories.SubscriptionRepository repository;

    public SubscriptionContextFacadeImpl(SubscriptionQueryService subscriptionQueryService,
            com.iotech.qualitrack.platform.subscription.domain.repositories.SubscriptionRepository repository) {
        this.subscriptionQueryService = subscriptionQueryService;
        this.repository = repository;
    }

    @Override
    public SubscriptionAccess getAccess(Long userId, Long laboratoryId) {
        var subscription = laboratoryId == null
                ? repository.findActiveByUserId(userId)
                : repository.findActiveByLaboratoryId(laboratoryId);
        return subscription.filter(value -> laboratoryId != null || value.getLaboratoryId() == null)
                .map(value -> new SubscriptionAccess(value.getId(), value.grantsAccess()))
                .orElse(new SubscriptionAccess(null, false));
    }

    @Override
    public void assignLaboratory(Long userId, Long laboratoryId) {
        var subscription = repository.findActiveByUserId(userId)
                .filter(value -> value.grantsAccess())
                .orElseThrow(() -> new IllegalStateException("An active subscription is required"));
        subscription.assignLaboratory(laboratoryId);
        repository.save(subscription);
    }

    @Override
    public boolean hasActiveSubscription(Long laboratoryId) {
        return subscriptionQueryService
                .handle(new GetActiveSubscriptionByLaboratoryIdQuery(laboratoryId))
                .filter(subscription -> subscription.grantsAccess()).isPresent();
    }

    @Override
    public boolean hasActivePlan(Long laboratoryId, String planCode) {
        return subscriptionQueryService
                .handle(new GetActiveSubscriptionByLaboratoryIdQuery(laboratoryId))
                .map(subscription -> subscription.getPlanCode().name().equals(planCode))
                .orElse(false);
    }

    @Override
    public String getActivePlanCode(Long laboratoryId) {
        return subscriptionQueryService
                .handle(new GetActiveSubscriptionByLaboratoryIdQuery(laboratoryId))
                .map(subscription -> subscription.getPlanCode().name())
                .orElse(null);
    }
}
