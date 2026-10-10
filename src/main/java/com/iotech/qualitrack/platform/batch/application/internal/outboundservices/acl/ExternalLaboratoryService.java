package com.iotech.qualitrack.platform.batch.application.internal.outboundservices.acl;

import com.iotech.qualitrack.platform.laboratory.interfaces.acl.LaboratoryContextFacade;
import org.springframework.stereotype.Service;

/**
 * ACL service used by the Batch bounded context to interact with Laboratory capabilities.
 */
@Service
public class ExternalLaboratoryService {

    private final LaboratoryContextFacade laboratoryContextFacade;

    /**
     * Creates the service with the Laboratory ACL facade dependency.
     *
     * @param laboratoryContextFacade laboratory bounded-context facade
     */
    public ExternalLaboratoryService(LaboratoryContextFacade laboratoryContextFacade) {
        this.laboratoryContextFacade = laboratoryContextFacade;
    }

    /**
     * Verifies that the environment is registered in the laboratory.
     *
     * @param laboratoryId the laboratory identifier
     * @param environmentId the environment identifier
     * @return true when the environment belongs to the laboratory
     */
    public boolean existsEnvironment(Long laboratoryId, Long environmentId) {
        return laboratoryContextFacade.existsEnvironment(laboratoryId, environmentId);
    }

    /**
     * Finds an environment of the laboratory with its usage.
     *
     * @return the environment, or empty when it does not belong to the laboratory
     */
    public java.util.Optional<LaboratoryContextFacade.EnvironmentReference> findEnvironment(Long laboratoryId, Long environmentId) {
        return laboratoryContextFacade.findEnvironment(laboratoryId, environmentId);
    }

    /**
     * Finds a staff member registered in the laboratory.
     *
     * @return the staff member, or empty when it is not registered in the laboratory
     */
    public java.util.Optional<LaboratoryContextFacade.StaffReference> findStaffMember(Long laboratoryId, Long staffId) {
        return laboratoryContextFacade.findStaffMember(laboratoryId, staffId);
    }
}