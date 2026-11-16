package com.iotech.qualitrack.platform.iam.domain.model.valueobjects;

/**
 * Subscription of an account as IAM needs it to resolve the onboarding.
 *
 * @param subscriptionId the subscription of the account, or null when it has none
 * @param active         true when the subscription currently grants access
 */
public record AccountSubscription(Long subscriptionId, boolean active) {
}
