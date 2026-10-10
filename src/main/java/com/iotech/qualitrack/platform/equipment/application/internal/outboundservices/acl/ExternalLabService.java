package com.iotech.qualitrack.platform.equipment.application.internal.outboundservices.acl;

import com.iotech.qualitrack.platform.laboratory.interfaces.acl.LaboratoryContextFacade;
import org.springframework.stereotype.Service;

/**
 * ACL service used by the Equipment bounded context to interact with Laboratory capabilities.
 */
@Service
public class ExternalLabService {

    private final LaboratoryContextFacade laboratoryContextFacade;

    /**
     * Creates the service with the Laboratory ACL facade dependency.
     *
     * @param laboratoryContextFacade laboratory bounded-context facade
     */
    public ExternalLabService(LaboratoryContextFacade laboratoryContextFacade) {
        this.laboratoryContextFacade = laboratoryContextFacade;
    }

    /**
     * Verifies if a laboratory exists by its ID through the Laboratory bounded context.
     *
     * @param labId the numeric identity of the laboratory
     * @return true if the laboratory exists, false otherwise
     */
    public boolean existsLaboratoryById(Long labId) {
        return laboratoryContextFacade.existsLaboratoryById(labId);
    }

    /**
     * Checks that the environment exists and belongs to the laboratory.
     */
    public boolean existsEnvironment(Long laboratoryId, Long environmentId) {
        return laboratoryContextFacade.existsEnvironment(laboratoryId, environmentId);
    }

    /**
     * Finds a staff member of the laboratory, for example the technician of a maintenance.
     */
    public java.util.Optional<LaboratoryContextFacade.StaffReference> findStaffMember(Long laboratoryId, Long staffId) {
        return laboratoryContextFacade.findStaffMember(laboratoryId, staffId);
    }
}
