package com.iotech.qualitrack.platform.ra.application.internal.outboundservices.acl;

import com.iotech.qualitrack.platform.equipment.interfaces.acl.EquipmentContextFacade;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Anti-corruption service that reads, through Equipment Management, the equipment and IoT devices of a laboratory.
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

    /**
     * Identifiers of all the equipment of the laboratory, located or not.
     */
    public List<Long> equipmentIds(Long laboratoryId) {
        return equipmentContextFacade.findLaboratoryEquipment(laboratoryId).stream()
                .map(EquipmentContextFacade.EquipmentReference::id).toList();
    }

    /**
     * An equipment located in the environment with its BPM parameter ranges and maintenance history, or empty when it
     * is not located there.
     */
    public Optional<EquipmentContextFacade.EquipmentLogReference> findEquipmentLog(Long laboratoryId, Long environmentId,
                                                                                    Long equipmentId) {
        return equipmentContextFacade.findEquipmentLog(laboratoryId, environmentId, equipmentId);
    }
}
