package com.iotech.qualitrack.platform.ra.application.internal.outboundservices.acl;

import com.iotech.qualitrack.platform.iam.interfaces.acl.IamContextFacade;
import org.springframework.stereotype.Service;

/**
 * Anti-corruption service that reads, through IAM, the accounts that generate the reports.
 */
@Service
public class RaExternalIamService {
    private final IamContextFacade iamContextFacade;

    public RaExternalIamService(IamContextFacade iamContextFacade) {
        this.iamContextFacade = iamContextFacade;
    }

    /**
     * Username of the account, shown as the author of a report.
     */
    public String username(Long userId) {
        return iamContextFacade.getUsernameByUserId(userId);
    }
}
