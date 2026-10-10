package com.iotech.qualitrack.platform.batch.interfaces.rest.transform;

import com.iotech.qualitrack.platform.batch.domain.model.entities.EquipmentUsage;
import com.iotech.qualitrack.platform.batch.domain.model.entities.StaffParticipation;
import com.iotech.qualitrack.platform.batch.interfaces.rest.resources.EquipmentUsageResource;
import com.iotech.qualitrack.platform.batch.interfaces.rest.resources.StaffParticipationResource;

/**
 * Maps equipment usages and staff participations to their REST representation.
 */
public final class BatchParticipationResourceFromEntityAssembler {

    private BatchParticipationResourceFromEntityAssembler() {
    }

    public static EquipmentUsageResource toResourceFromEntity(EquipmentUsage usage) {
        return new EquipmentUsageResource(usage.getId(), usage.getBatchId(), usage.getEquipmentId(), usage.getEquipmentName(),
                usage.getRegisteredByUserId(), usage.getRegisteredAt());
    }

    public static StaffParticipationResource toResourceFromEntity(StaffParticipation participation) {
        return new StaffParticipationResource(participation.getId(), participation.getBatchId(), participation.getStaffId(),
                participation.getStaffName(), participation.getStaffRole(), participation.getRegisteredByUserId(),
                participation.getRegisteredAt());
    }
}
