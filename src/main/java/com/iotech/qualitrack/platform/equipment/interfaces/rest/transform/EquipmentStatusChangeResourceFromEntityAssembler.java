package com.iotech.qualitrack.platform.equipment.interfaces.rest.transform;

import com.iotech.qualitrack.platform.equipment.domain.model.entities.EquipmentStatusChange;
import com.iotech.qualitrack.platform.equipment.interfaces.rest.resources.EquipmentStatusChangeResource;

/**
 * Assembler to convert an EquipmentStatusChange into its REST resource.
 */
public class EquipmentStatusChangeResourceFromEntityAssembler {

    public static EquipmentStatusChangeResource toResourceFromEntity(EquipmentStatusChange change) {
        return new EquipmentStatusChangeResource(change.getId(), change.getEquipmentId(), change.getEnvironmentId(),
                change.getPreviousStatus().name(), change.getNewStatus().name(), change.getReason(),
                change.getChangedByUserId(), change.getChangedAt().toString());
    }
}
