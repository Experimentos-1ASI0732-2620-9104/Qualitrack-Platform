package com.iotech.qualitrack.platform.laboratory.infrastructure.persistence.jpa.assemblers;

import com.iotech.qualitrack.platform.laboratory.domain.model.aggregates.Environment;
import com.iotech.qualitrack.platform.laboratory.infrastructure.persistence.jpa.entities.EnvironmentPersistenceEntity;

/**
 * Static assembler between environment domain and persistence representations.
 */
public final class EnvironmentPersistenceAssembler {

    private EnvironmentPersistenceAssembler() {
    }

    public static Environment toDomainFromPersistence(EnvironmentPersistenceEntity entity) {
        if (entity == null) return null;

        return new Environment(
                entity.getId(),
                entity.getLaboratoryId(),
                entity.getCode(),
                entity.getName(),
                entity.getDescription(),
                entity.getUsage(),
                entity.getUsageAssignedBy(),
                entity.getUsageAssignedAt()
        );
    }

    public static EnvironmentPersistenceEntity toPersistenceFromDomain(Environment environment,
                                                                       EnvironmentPersistenceEntity entity) {
        if (environment == null) return null;

        var target = entity == null ? new EnvironmentPersistenceEntity() : entity;
        if (environment.getId() != null) {
            target.setId(environment.getId());
        }
        target.setLaboratoryId(environment.getLaboratoryId());
        target.setCode(environment.getCode());
        target.setName(environment.getName());
        target.setDescription(environment.getDescription());
        target.setUsage(environment.getUsage());
        target.setUsageAssignedBy(environment.getUsageAssignedBy());
        target.setUsageAssignedAt(environment.getUsageAssignedAt());
        return target;
    }
}
