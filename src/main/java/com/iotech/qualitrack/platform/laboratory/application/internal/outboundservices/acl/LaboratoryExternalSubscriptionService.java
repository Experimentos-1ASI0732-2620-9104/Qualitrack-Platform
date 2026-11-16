package com.iotech.qualitrack.platform.laboratory.application.internal.outboundservices.acl;

import com.iotech.qualitrack.platform.subscription.interfaces.acl.SubscriptionContextFacade;
import org.springframework.stereotype.Service;

/**
 * ACL service used by Laboratory Management to check and associate the subscription of the owner when the laboratory is
 * created.
 */
@Service
public class LaboratoryExternalSubscriptionService {
    private final SubscriptionContextFacade subscriptionContextFacade;

    public LaboratoryExternalSubscriptionService(SubscriptionContextFacade subscriptionContextFacade) {
        this.subscriptionContextFacade = subscriptionContextFacade;
    }

    /**
     * @return true when the user has an active subscription not yet associated with a laboratory
     */
    public boolean hasActiveSubscription(Long userId) {
        return subscriptionContextFacade.getAccess(userId, null).active();
    }

    /**
     * Associates the active subscription of the user with the new laboratory.
     */
    public void assignLaboratory(Long userId, Long laboratoryId) {
        subscriptionContextFacade.assignLaboratory(userId, laboratoryId);
    }
}
