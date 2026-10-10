package com.iotech.qualitrack.platform.tracking.application.internal.outboundservices.acl;

import com.iotech.qualitrack.platform.equipment.interfaces.acl.EquipmentContextFacade;
import com.iotech.qualitrack.platform.equipment.interfaces.acl.EquipmentContextFacade.DeviceReference;
import com.iotech.qualitrack.platform.tracking.domain.model.valueobjects.MonitoredMetric.DeviceKind;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Anti-corruption layer to Equipment Management: identity, type and location of the IoT devices whose readings,
 * profiles and actions Tracking &amp; Telemetry records.
 */
@Service
public class TrackingExternalEquipmentService {
    private final EquipmentContextFacade equipmentContextFacade;

    public TrackingExternalEquipmentService(EquipmentContextFacade equipmentContextFacade) {
        this.equipmentContextFacade = equipmentContextFacade;
    }

    /**
     * Any IoT device located in the environment.
     */
    public Optional<DeviceReference> findDevice(Long laboratoryId, Long environmentId, Long deviceId) {
        return equipmentContextFacade.findDevice(laboratoryId, environmentId, deviceId);
    }

    /**
     * The environmental device that supervises the environment.
     */
    public Optional<DeviceReference> findEnvironmentalDevice(Long laboratoryId, Long environmentId) {
        return equipmentContextFacade.findEnvironmentalDevice(laboratoryId, environmentId);
    }

    /**
     * A container monitor located in the environment.
     */
    public Optional<DeviceReference> findContainerMonitor(Long laboratoryId, Long environmentId, Long deviceId) {
        return findDevice(laboratoryId, environmentId, deviceId)
                .filter(device -> DeviceKind.CONTAINER_MONITOR.name().equals(device.deviceType()));
    }
}
