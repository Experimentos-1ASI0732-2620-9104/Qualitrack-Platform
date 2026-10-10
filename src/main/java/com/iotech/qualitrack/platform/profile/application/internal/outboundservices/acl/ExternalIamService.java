package com.iotech.qualitrack.platform.profile.application.internal.outboundservices.acl;

import com.iotech.qualitrack.platform.iam.interfaces.acl.IamContextFacade;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Reads the sign-in account a profile belongs to.
 */
@Service("profileExternalIamService")
public class ExternalIamService {

    private final IamContextFacade iamContextFacade;

    public ExternalIamService(IamContextFacade iamContextFacade) {
        this.iamContextFacade = iamContextFacade;
    }

    public Optional<Account> findAccount(Long userId) {
        return iamContextFacade.findAccount(userId)
                .map(account -> new Account(account.userId(), account.username(), account.email(), account.roles()));
    }

    /**
     * Account of a profile.
     */
    public record Account(Long userId, String username, String email, List<String> roles) {
    }
}
