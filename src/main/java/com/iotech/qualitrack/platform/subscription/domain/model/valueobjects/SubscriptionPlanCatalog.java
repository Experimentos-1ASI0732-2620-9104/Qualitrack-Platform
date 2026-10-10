package com.iotech.qualitrack.platform.subscription.domain.model.valueobjects;

import com.iotech.qualitrack.platform.subscription.domain.model.entities.SubscriptionPlan;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/**
 * Plans QualiTrack offers to laboratories, created when the database does not have them yet.
 *
 * @param plans new plans, one per plan code and billing cycle, each with its own Stripe price
 */
public record SubscriptionPlanCatalog(List<SubscriptionPlan> plans) {

    /**
     * Creates a validated catalog.
     */
    public SubscriptionPlanCatalog {
        plans = List.copyOf(Objects.requireNonNull(plans, "Plans are required"));
        var offers = new HashSet<String>();
        var stripePrices = new HashSet<String>();
        for (var plan : plans) {
            if (plan.getId() != null) {
                throw new IllegalArgumentException("The catalog only holds plans that are not stored yet");
            }
            if (!offers.add(plan.getCode() + "/" + plan.getBillingCycle())) {
                throw new IllegalArgumentException("Each plan code and billing cycle is offered only once");
            }
            if (plan.getStripePriceId() != null && !stripePrices.add(plan.getStripePriceId())) {
                throw new IllegalArgumentException("Each plan needs its own Stripe price");
            }
        }
    }
}
