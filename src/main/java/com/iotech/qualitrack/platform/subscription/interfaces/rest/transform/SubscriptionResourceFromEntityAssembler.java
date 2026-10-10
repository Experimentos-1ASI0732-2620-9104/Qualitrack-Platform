package com.iotech.qualitrack.platform.subscription.interfaces.rest.transform;

import com.iotech.qualitrack.platform.subscription.domain.model.aggregates.Subscription;
import com.iotech.qualitrack.platform.subscription.interfaces.rest.resources.SubscriptionResource;

/**
 * Assembler that maps Subscription domain aggregates to REST resources.
 */
public class SubscriptionResourceFromEntityAssembler {

    public static SubscriptionResource toResourceFromEntity(Subscription entity) {
        return new SubscriptionResource(
                entity.getId(),
                entity.getUserId(),
                entity.getLaboratoryId(),
                entity.getPlanCode().name(),
                entity.getBillingCycle().name(),
                entity.getStatus().name(),
                entity.getStripeCustomerId(),
                entity.getStripeSubscriptionId(),
                entity.getStripeCheckoutSessionId(),
                entity.getCurrentPeriodStart(),
                entity.getCurrentPeriodEnd(),
                entity.getCancelledAt(),
                entity.getCancelledBy(),
                entity.isCancelAtPeriodEnd()
        );
    }
}