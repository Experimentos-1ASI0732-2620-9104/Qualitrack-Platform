package com.iotech.qualitrack.platform.subscription.interfaces.acl;

/**
 * ACL facade exposed by the Subscription bounded context.
 *
 * <p>Provides a stable interface for other bounded contexts to verify
 * subscription capabilities without depending on subscription internals.</p>
 */
public interface SubscriptionContextFacade {

    /**
     * Subscription of an account: the one of its laboratory, or the one not yet associated with a laboratory when
     * laboratoryId is null.
     *
     * @return the subscription and whether it currently grants access; no subscription and no access when there is none
     */
    SubscriptionAccess getAccess(Long userId, Long laboratoryId);

    /**
     * Associates the active subscription of the user with the laboratory created during the onboarding.
     *
     * @throws IllegalStateException when the user has no active subscription
     */
    void assignLaboratory(Long userId, Long laboratoryId);

    /**
     * Checks whether a laboratory currently has an active subscription.
     *
     * @param laboratoryId laboratory identifier
     * @return true if the laboratory has an active subscription, otherwise false
     */
    boolean hasActiveSubscription(Long laboratoryId);

    /**
     * Checks whether a laboratory has an active subscription with the given plan.
     *
     * @param laboratoryId laboratory identifier
     * @param planCode expected plan code: FREE, BASIC, PROFESSIONAL or ENTERPRISE
     * @return true if the active subscription matches the requested plan
     */
    boolean hasActivePlan(Long laboratoryId, String planCode);

    /**
     * Gets the current active plan code for a laboratory.
     *
     * @param laboratoryId laboratory identifier
     * @return active plan name, or null if no active subscription exists
     */
    String getActivePlanCode(Long laboratoryId);
}
