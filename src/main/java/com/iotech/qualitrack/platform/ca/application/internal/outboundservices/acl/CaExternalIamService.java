package com.iotech.qualitrack.platform.ca.application.internal.outboundservices.acl;

import com.iotech.qualitrack.platform.iam.interfaces.acl.IamContextFacade;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Reads the accounts of a laboratory, who receive its notifications.
 */
@Service
public class CaExternalIamService {

    private final IamContextFacade iamContextFacade;

    public CaExternalIamService(IamContextFacade iamContextFacade) {
        this.iamContextFacade = iamContextFacade;
    }

    /**
     * @return the accounts of the laboratory that can sign in
     */
    public List<Recipient> findRecipients(Long laboratoryId) {
        return iamContextFacade.findActiveAccounts(laboratoryId).stream()
                .map(account -> new Recipient(account.userId(), account.email()))
                .toList();
    }

    /**
     * Person who can receive notifications.
     *
     * @param userId the account
     * @param email e-mail of the account, or null for old accounts without it
     */
    public record Recipient(Long userId, String email) {
    }
}
