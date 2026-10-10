package com.iotech.qualitrack.platform.laboratory.application.internal.outboundservices.acl;

import com.iotech.qualitrack.platform.iam.interfaces.acl.IamContextFacade;
import com.iotech.qualitrack.platform.laboratory.application.commandservices.StaffCommandService;
import com.iotech.qualitrack.platform.laboratory.domain.model.valueobjects.StaffAccessRole;
import org.springframework.stereotype.Service;

/**
 * ACL service used by Laboratory Management to create and disable the accounts of its staff in IAM.
 */
@Service
public class ExternalIamService {
    private final IamContextFacade iamContextFacade;

    public ExternalIamService(IamContextFacade iamContextFacade) {
        this.iamContextFacade = iamContextFacade;
    }

    public boolean existsAccount(String email) {
        return iamContextFacade.existsUsername(email);
    }

    /**
     * Creates the account of the staff member and sends the credentials.
     *
     * @throws com.iotech.qualitrack.platform.shared.application.result.ApplicationException CONFLICT when the e-mail
     * already identifies another account
     */
    public StaffCommandService.StaffAccount createAccount(Long laboratoryId, String email, String fullName,
                                                          StaffAccessRole accessRole) {
        var account = iamContextFacade.createStaffAccount(laboratoryId, email, fullName, accessRole == StaffAccessRole.AUDITOR);
        return new StaffCommandService.StaffAccount(account.userId(), account.username(), account.temporaryPassword(),
                account.credentialsSent());
    }

    public void disableAccount(Long userId) {
        if (userId != null) iamContextFacade.deactivateUser(userId);
    }
}
