package com.iotech.qualitrack.platform.laboratory.interfaces.rest.transform;

import com.iotech.qualitrack.platform.laboratory.domain.model.aggregates.Environment;
import com.iotech.qualitrack.platform.laboratory.interfaces.rest.resources.EnvironmentResource;
import com.iotech.qualitrack.platform.laboratory.interfaces.rest.resources.EnvironmentUsageAssignmentResource;

/**
 * Assembler to convert Environment aggregates to REST resources.
 */
public final class EnvironmentResourceFromEntityAssembler {

    private EnvironmentResourceFromEntityAssembler() {
    }

    public static EnvironmentResource toResourceFromEntity(Environment entity) {
        return new EnvironmentResource(
                entity.getId(),
                entity.getLaboratoryId(),
                entity.getCode(),
                entity.getName(),
                entity.getDescription(),
                entity.getUsage() == null ? null : entity.getUsage().name(),
                entity.getUsageAssignedBy(),
                entity.getUsageAssignedAt()
        );
    }

    public static EnvironmentUsageAssignmentResource toUsageAssignmentResourceFromEntity(Environment entity) {
        return new EnvironmentUsageAssignmentResource(
                entity.getId(),
                entity.getLaboratoryId(),
                entity.getUsage().name(),
                entity.getUsageAssignedBy(),
                entity.getUsageAssignedAt()
        );
    }
}
