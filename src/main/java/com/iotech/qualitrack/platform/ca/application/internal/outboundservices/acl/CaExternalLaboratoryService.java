package com.iotech.qualitrack.platform.ca.application.internal.outboundservices.acl;

import com.iotech.qualitrack.platform.laboratory.interfaces.acl.LaboratoryContextFacade;
import org.springframework.stereotype.Service;

/**
 * Reads the names of the environments the alerts belong to.
 */
@Service
public class CaExternalLaboratoryService {

    private final LaboratoryContextFacade laboratoryContextFacade;

    public CaExternalLaboratoryService(LaboratoryContextFacade laboratoryContextFacade) {
        this.laboratoryContextFacade = laboratoryContextFacade;
    }

    /**
     * @return the name of the environment, or null when it does not exist
     */
    public String environmentName(Long laboratoryId, Long environmentId) {
        if (laboratoryId == null || environmentId == null) return null;
        return laboratoryContextFacade.findEnvironment(laboratoryId, environmentId)
                .map(LaboratoryContextFacade.EnvironmentReference::name)
                .orElse(null);
    }
}
