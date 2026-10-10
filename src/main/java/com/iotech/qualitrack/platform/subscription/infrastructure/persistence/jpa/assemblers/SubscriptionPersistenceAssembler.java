package com.iotech.qualitrack.platform.subscription.infrastructure.persistence.jpa.assemblers;

import com.iotech.qualitrack.platform.subscription.domain.model.aggregates.Subscription;
import com.iotech.qualitrack.platform.subscription.infrastructure.persistence.jpa.entities.SubscriptionPersistenceEntity;

/**
 * Assembler for subscription persistence mapping.
 */
public class SubscriptionPersistenceAssembler {

    private SubscriptionPersistenceAssembler() {
    }

    public static Subscription toDomainFromPersistence(SubscriptionPersistenceEntity entity) {
        if (entity == null) return null;

        return new Subscription(
                entity.getId(),
                entity.getUserId(),
                entity.getLaboratoryId(),
                entity.getPlanCode(),
                entity.getBillingCycle(),
                entity.getStatus(),
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

    public static SubscriptionPersistenceEntity toPersistenceFromDomain(Subscription subscription) {
        if (subscription == null) return null;

        var entity = new SubscriptionPersistenceEntity();
        entity.setId(subscription.getId());
        entity.setUserId(subscription.getUserId());
        entity.setLaboratoryId(subscription.getLaboratoryId());
        entity.setPlanCode(subscription.getPlanCode());
        entity.setBillingCycle(subscription.getBillingCycle());
        entity.setStatus(subscription.getStatus());
        entity.setStripeCustomerId(subscription.getStripeCustomerId());
        entity.setStripeSubscriptionId(subscription.getStripeSubscriptionId());
        entity.setStripeCheckoutSessionId(subscription.getStripeCheckoutSessionId());
        entity.setCurrentPeriodStart(subscription.getCurrentPeriodStart());
        entity.setCurrentPeriodEnd(subscription.getCurrentPeriodEnd());
        entity.setCancelledAt(subscription.getCancelledAt());
        entity.setCancelledBy(subscription.getCancelledBy());
        entity.setCancelAtPeriodEnd(subscription.isCancelAtPeriodEnd());

        return entity;
    }
}