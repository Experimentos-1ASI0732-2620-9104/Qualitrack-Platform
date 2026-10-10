package com.iotech.qualitrack.platform.subscription.application.internal.outboundservices.acl;

/**
 * Stripe subscription details required to activate a local subscription.
 *
 * @param stripeCustomerId Stripe customer identifier
 * @param stripeSubscriptionId Stripe subscription identifier
 * @param currentPeriodStart current billing period start date
 * @param currentPeriodEnd current billing period end date
 * @param status Stripe status of the subscription
 * @param cancelAtPeriodEnd whether Stripe ends the subscription with the current period instead of renewing it
 */
public record StripeSubscriptionDetails(
        String stripeCustomerId,
        String stripeSubscriptionId,
        String currentPeriodStart,
        String currentPeriodEnd,
        String status,
        boolean cancelAtPeriodEnd
) {
}
