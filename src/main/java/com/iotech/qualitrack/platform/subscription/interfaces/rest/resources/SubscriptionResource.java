package com.iotech.qualitrack.platform.subscription.interfaces.rest.resources;

/**
 * Resource representing a subscription exposed through the REST API.
 */
public record SubscriptionResource(
        Long id,
        Long userId,
        Long laboratoryId,
        String planCode,
        String billingCycle,
        String status,
        String stripeCustomerId,
        String stripeSubscriptionId,
        String stripeCheckoutSessionId,
        String currentPeriodStart,
        String currentPeriodEnd,
        String cancelledAt,
        Long cancelledBy,
        boolean cancelAtPeriodEnd
) {
}