package com.iotech.qualitrack.platform.subscription.domain.model.queries;

/**
 * Query used to retrieve a subscription by its identifier.
 *
 * @param subscriptionId The internal subscription identifier.
 */
public record GetSubscriptionByIdQuery(Long subscriptionId) {

    public GetSubscriptionByIdQuery {
        if (subscriptionId == null || subscriptionId <= 0) {
            throw new IllegalArgumentException("Subscription id must be a positive number");
        }
    }
}
