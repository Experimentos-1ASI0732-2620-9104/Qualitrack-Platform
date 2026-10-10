package com.iotech.qualitrack.platform.profile.application.internal.outboundservices.acl;

import com.iotech.qualitrack.platform.laboratory.interfaces.acl.LaboratoryContextFacade;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Reads the staff records of the laboratory that profiles are linked to through the account.
 */
@Service("profileExternalLaboratoryService")
public class ExternalLaboratoryService {

    private final LaboratoryContextFacade laboratoryContextFacade;

    public ExternalLaboratoryService(LaboratoryContextFacade laboratoryContextFacade) {
        this.laboratoryContextFacade = laboratoryContextFacade;
    }

    /**
     * @param userId the account
     * @return the staff record of the person, if the account belongs to a staff member
     */
    public Optional<StaffMember> findStaffMemberByAccount(Long userId) {
        return laboratoryContextFacade.findStaffMemberByAccount(userId)
                .map(member -> new StaffMember(member.id(), member.userId(), member.fullName(), member.role()));
    }

    /**
     * @return the staff member of the laboratory, if it exists
     */
    public Optional<StaffMember> findStaffMember(Long laboratoryId, Long staffId) {
        return laboratoryContextFacade.findStaffMember(laboratoryId, staffId)
                .map(member -> new StaffMember(member.id(), member.userId(), member.fullName(), member.role()));
    }

    /**
     * Staff record seen from the profile context.
     *
     * @param staffId the staff record
     * @param userId the account, or null when the staff member has no account
     * @param fullName name registered by the quality manager
     * @param position position registered by the quality manager
     */
    public record StaffMember(Long staffId, Long userId, String fullName, String position) {
    }
}
