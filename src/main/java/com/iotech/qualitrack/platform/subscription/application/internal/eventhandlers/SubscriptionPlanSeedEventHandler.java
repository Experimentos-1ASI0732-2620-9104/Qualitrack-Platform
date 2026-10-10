package com.iotech.qualitrack.platform.subscription.application.internal.eventhandlers;

import com.iotech.qualitrack.platform.shared.application.result.ApplicationError;
import com.iotech.qualitrack.platform.shared.application.result.Result;
import com.iotech.qualitrack.platform.subscription.application.commandservices.SubscriptionPlanCommandService;
import com.iotech.qualitrack.platform.subscription.domain.model.commands.SeedSubscriptionPlansCommand;
import com.iotech.qualitrack.platform.subscription.domain.model.valueobjects.SubscriptionPlanCatalog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/**
 * Creates the subscription plans of the catalog when the application is ready, so an empty
 * database offers them without loading them by hand.
 */
@Service
@Slf4j
public class SubscriptionPlanSeedEventHandler {

    private final SubscriptionPlanCommandService subscriptionPlanCommandService;
    private final SubscriptionPlanCatalog subscriptionPlanCatalog;

    public SubscriptionPlanSeedEventHandler(SubscriptionPlanCommandService subscriptionPlanCommandService,
                                            SubscriptionPlanCatalog subscriptionPlanCatalog) {
        this.subscriptionPlanCommandService = subscriptionPlanCommandService;
        this.subscriptionPlanCatalog = subscriptionPlanCatalog;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        switch (subscriptionPlanCommandService.handle(new SeedSubscriptionPlansCommand(subscriptionPlanCatalog))) {
            case Result.Success<Integer, ApplicationError> success ->
                    log.info("Subscription plan seed completed. Created plans: {}.", success.value());
            case Result.Failure<Integer, ApplicationError> failure ->
                    log.warn("Subscription plan seed failed: {}", failure.error().details());
        }
    }
}
