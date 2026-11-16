package com.iotech.qualitrack.platform.iam.application.internal.outboundservices.acl;

import com.iotech.qualitrack.platform.iam.domain.model.valueobjects.AccountSubscription;
import com.iotech.qualitrack.platform.subscription.interfaces.acl.SubscriptionContextFacade;
import org.springframework.stereotype.Service;

/**
 * ACL service used by IAM to read, through Payments &amp; Subscriptions, whether the subscription of an account grants
 * access. The access depends on the end of the current period, which only Payments &amp; Subscriptions keeps.
 */
@Service
public class IamExternalSubscriptionService {
    private final SubscriptionContextFacade subscriptionContextFacade;

    public IamExternalSubscriptionService(SubscriptionContextFacade subscriptionContextFacade) {
        this.subscriptionContextFacade = subscriptionContextFacade;
    }

    /**
     * Subscription of the account: the one of its laboratory, or the one not yet associated with a laboratory when the
     * account has none.
     */
    public AccountSubscription findSubscription(Long userId, Long laboratoryId) {
        var access = subscriptionContextFacade.getAccess(userId, laboratoryId);
        return new AccountSubscription(access.subscriptionId(), access.active());
    }
}
