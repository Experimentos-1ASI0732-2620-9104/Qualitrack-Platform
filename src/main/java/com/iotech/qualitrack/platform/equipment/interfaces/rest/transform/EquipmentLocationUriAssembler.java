package com.iotech.qualitrack.platform.equipment.interfaces.rest.transform;

import com.iotech.qualitrack.platform.equipment.domain.model.aggregates.Equipment;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/**
 * Builds the canonical URI of an equipment or IoT device: {@code /api/v1/laboratories/{laboratoryId}/equipments/{equipmentId}}.
 */
public final class EquipmentLocationUriAssembler {
    private EquipmentLocationUriAssembler() {
    }

    public static URI toUriFromEntity(Equipment equipment) {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/laboratories/{laboratoryId}/equipments/{equipmentId}")
                .buildAndExpand(equipment.getLabId(), equipment.getId())
                .toUri();
    }
}
