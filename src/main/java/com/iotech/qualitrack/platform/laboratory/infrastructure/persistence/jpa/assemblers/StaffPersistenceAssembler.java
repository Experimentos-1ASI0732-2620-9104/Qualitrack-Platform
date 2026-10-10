package com.iotech.qualitrack.platform.laboratory.infrastructure.persistence.jpa.assemblers;

import com.iotech.qualitrack.platform.laboratory.domain.model.aggregates.StaffMember;
import com.iotech.qualitrack.platform.laboratory.infrastructure.persistence.jpa.entities.StaffPersistenceEntity;

/**
 * Static assembler between staff member domain and persistence representations.
 */
public final class StaffPersistenceAssembler {

    private StaffPersistenceAssembler() {
    }

    public static StaffMember toDomainFromPersistence(StaffPersistenceEntity entity) {
        if (entity == null) return null;

        return new StaffMember(
                entity.getId(),
                entity.getLaboratoryId(),
                entity.getFullName(),
                entity.getRole(),
                entity.getEmail(),
                entity.isActive(),
                entity.getAccessRole(),
                entity.getUserId()
        );
    }

    public static StaffPersistenceEntity toPersistenceFromDomain(StaffMember staffMember) {
        if (staffMember == null) return null;

        var entity = new StaffPersistenceEntity();

        // Only set ID if the staff member is being updated (has a non-null ID)
        // For new staff members, leave ID null to allow JPA to generate it
        if (staffMember.getId() != null) {
            entity.setId(staffMember.getId());
        }

        entity.setLaboratoryId(staffMember.getLaboratoryId());
        entity.setFullName(staffMember.getFullName());
        entity.setRole(staffMember.getRole());
        entity.setEmail(staffMember.getEmail());
        entity.setActive(staffMember.isActive());
        entity.setAccessRole(staffMember.getAccessRole());
        entity.setUserId(staffMember.getUserId());

        return entity;
    }
}