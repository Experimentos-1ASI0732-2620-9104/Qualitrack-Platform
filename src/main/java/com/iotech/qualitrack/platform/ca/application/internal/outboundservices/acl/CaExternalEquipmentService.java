package com.iotech.qualitrack.platform.ca.application.internal.outboundservices.acl;

import com.iotech.qualitrack.platform.ca.domain.model.valueobjects.AlertOrigin;
import com.iotech.qualitrack.platform.equipment.interfaces.acl.EquipmentContextFacade;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Anti-corruption service that resolves, through Equipment Management, the IoT device where a deviation originated.
 */
@Service
public class CaExternalEquipmentService {

    private final EquipmentContextFacade equipmentContextFacade;

    public CaExternalEquipmentService(EquipmentContextFacade equipmentContextFacade) {
        this.equipmentContextFacade = equipmentContextFacade;
    }

    /**
     * Finds the IoT device of an environment that detected a deviation.
     *
     * @param laboratoryId the laboratory of the environment
     * @param environmentId the environment
     * @param deviceId the environmental device or container monitor; null means the environmental device of the
     *                 environment
     * @return the device with the origin it supervises, or empty when it is not an IoT device located there
     */
    public Optional<AlertSource> findSource(Long laboratoryId, Long environmentId, Long deviceId) {
        var device = deviceId == null
                ? equipmentContextFacade.findEnvironmentalDevice(laboratoryId, environmentId)
                : equipmentContextFacade.findDevice(laboratoryId, environmentId, deviceId);
        return device.map(found -> new AlertSource(found.id(), found.name(), AlertOrigin.fromDeviceType(found.deviceType())));
    }

    /**
     * IoT device where an alert originates.
     *
     * @param deviceId the environmental device or container monitor
     * @param name the device name
     * @param origin whether it supervises the environment or a container
     */
    public record AlertSource(Long deviceId, String name, AlertOrigin origin) {
    }
}
