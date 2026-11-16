package com.iotech.qualitrack.platform.subscription.application.internal.outboundservices.acl;

import com.iotech.qualitrack.platform.iam.interfaces.acl.IamContextFacade;
import org.springframework.stereotype.Service;

/**
 * ACL service used by Payments &amp; Subscriptions to serialize the changes of a subscription per account in IAM.
 */
@Service
public class SubscriptionExternalIamService {
    private final IamContextFacade iamContextFacade;

    public SubscriptionExternalIamService(IamContextFacade iamContextFacade) {
        this.iamContextFacade = iamContextFacade;
    }

    /**
     * Locks the account until the transaction ends.
     *
     * @return the laboratory associated with the account, or null when it has none yet
     */
    public Long lockLaboratoryAssociation(Long userId) {
        return iamContextFacade.lockLaboratoryAssociation(userId);
    }
}
