package com.iotech.qualitrack.platform.subscription.application.internal.commandservices;

import com.iotech.qualitrack.platform.shared.application.result.ApplicationError;
import com.iotech.qualitrack.platform.shared.application.result.Result;
import com.iotech.qualitrack.platform.subscription.application.commandservices.SubscriptionPlanCommandService;
import com.iotech.qualitrack.platform.subscription.domain.model.commands.SeedSubscriptionPlansCommand;
import com.iotech.qualitrack.platform.subscription.domain.repositories.SubscriptionPlanRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Application command service implementation for subscription plans.
 */
@Service
@Slf4j
public class SubscriptionPlanCommandServiceImpl implements SubscriptionPlanCommandService {

    private final SubscriptionPlanRepository subscriptionPlanRepository;

    public SubscriptionPlanCommandServiceImpl(SubscriptionPlanRepository subscriptionPlanRepository) {
        this.subscriptionPlanRepository = subscriptionPlanRepository;
    }

    /**
     * Stored plans are never changed: once created, the database is the source of the plan.
     */
    @Override
    public Result<Integer, ApplicationError> handle(SeedSubscriptionPlansCommand command) {
        try {
            int createdPlans = 0;

            for (var plan : command.catalog().plans()) {
                if (subscriptionPlanRepository.findByCodeAndBillingCycle(plan.getCode(), plan.getBillingCycle()).isPresent()) {
                    continue;
                }
                if (plan.getStripePriceId() != null
                        && subscriptionPlanRepository.findByStripePriceId(plan.getStripePriceId()).isPresent()) {
                    log.warn("Subscription plan {} {} was not created: another plan already uses its Stripe price.",
                            plan.getCode(), plan.getBillingCycle());
                    continue;
                }
                subscriptionPlanRepository.save(plan);
                createdPlans++;
            }

            return Result.success(createdPlans);

        } catch (Exception e) {
            return Result.failure(ApplicationError.unexpected("seed-subscription-plans", e.getMessage()));
        }
    }
}
