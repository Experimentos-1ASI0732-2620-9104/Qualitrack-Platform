package com.iotech.qualitrack.platform.inventory.application.internal.outboundservices.acl;

import com.iotech.qualitrack.platform.equipment.interfaces.acl.EquipmentContextFacade;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Outbound ACL used by Inventory to read the monitored containers owned by Equipment Management.
 */
@Service
public class InventoryExternalEquipmentService {

    private final EquipmentContextFacade equipmentContextFacade;

    public InventoryExternalEquipmentService(EquipmentContextFacade equipmentContextFacade) {
        this.equipmentContextFacade = equipmentContextFacade;
    }

    /**
     * Finds a container monitor of the laboratory, which represents a monitored container.
     *
     * @return the container, or empty when the device is not a container monitor of the laboratory
     */
    public Optional<EquipmentContextFacade.ContainerReference> findContainerMonitor(Long laboratoryId, Long containerMonitorId) {
        return equipmentContextFacade.findContainerMonitor(laboratoryId, containerMonitorId);
    }
}
