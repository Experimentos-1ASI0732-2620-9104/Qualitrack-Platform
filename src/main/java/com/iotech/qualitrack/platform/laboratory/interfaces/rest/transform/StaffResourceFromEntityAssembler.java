package com.iotech.qualitrack.platform.laboratory.interfaces.rest.transform;

import com.iotech.qualitrack.platform.laboratory.application.commandservices.StaffCommandService;
import com.iotech.qualitrack.platform.laboratory.domain.model.aggregates.StaffMember;
import com.iotech.qualitrack.platform.laboratory.interfaces.rest.resources.RegisteredStaffResource;
import com.iotech.qualitrack.platform.laboratory.interfaces.rest.resources.StaffCredentialsResource;
import com.iotech.qualitrack.platform.laboratory.interfaces.rest.resources.StaffMemberResource;

/**
 * Assembler to convert staff members into REST resources.
 */
public class StaffResourceFromEntityAssembler {

    public static StaffMemberResource toResourceFromEntity(StaffMember entity) {
        return new StaffMemberResource(
                entity.getId(),
                entity.getLaboratoryId(),
                entity.getFullName(),
                entity.getRole(),
                entity.getEmail(),
                entity.isActive(),
                entity.getAccessRole() == null ? null : entity.getAccessRole().name(),
                entity.getUserId()
        );
    }

    /**
     * The temporary password is only included when it was not e-mailed, so the quality manager can hand it over.
     */
    public static RegisteredStaffResource toResourceFromRegistration(StaffCommandService.RegisteredStaff registered) {
        var account = registered.account();
        var credentials = new StaffCredentialsResource(account.username(),
                account.credentialsSent() ? "EMAIL" : "SHOWN_ONCE",
                account.credentialsSent() ? null : account.temporaryPassword());
        return new RegisteredStaffResource(toResourceFromEntity(registered.staffMember()), credentials);
    }
}
