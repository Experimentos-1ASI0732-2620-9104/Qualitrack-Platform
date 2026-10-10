package com.iotech.qualitrack.platform.ra.application.internal.outboundservices.acl;

import com.iotech.qualitrack.platform.equipment.interfaces.acl.EquipmentContextFacade;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Anti-corruption service that reads, through Equipment Management, the IoT devices of a laboratory.
 */
@Service
public class RaExternalEquipmentService {
    private final EquipmentContextFacade equipmentContextFacade;

    public RaExternalEquipmentService(EquipmentContextFacade equipmentContextFacade) {
        this.equipmentContextFacade = equipmentContextFacade;
    }

    /**
     * Names of the IoT devices of the laboratory, by device.
     */
    public Map<Long, String> deviceNames(Long laboratoryId) {
        return equipmentContextFacade.findDevices(laboratoryId).stream()
                .collect(Collectors.toMap(EquipmentContextFacade.DeviceReference::id,
                        EquipmentContextFacade.DeviceReference::name, (first, second) -> first));
    }
}
