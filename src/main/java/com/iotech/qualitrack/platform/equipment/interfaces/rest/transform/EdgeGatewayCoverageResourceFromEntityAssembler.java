package com.iotech.qualitrack.platform.equipment.interfaces.rest.transform;

import com.iotech.qualitrack.platform.equipment.domain.model.aggregates.Equipment;
import com.iotech.qualitrack.platform.equipment.domain.services.EdgeGatewayAllocation;
import com.iotech.qualitrack.platform.equipment.interfaces.rest.resources.EdgeGatewayCoverageResource;

import java.util.Collection;
import java.util.Comparator;

/**
 * Builds the Edge Gateway coverage of an environment from the equipment located in it.
 */
public class EdgeGatewayCoverageResourceFromEntityAssembler {

    /**
     * Summarizes how the Gateways of the environment cover its IoT devices.
     *
     * @param environmentEquipment everything located in one environment
     * @return the Gateways with the number of devices each one serves, and the devices without a Gateway
     */
    public static EdgeGatewayCoverageResource toResourceFromEntities(Collection<Equipment> environmentEquipment) {
        var allocation = EdgeGatewayAllocation.ofEnvironment(environmentEquipment);
        var gateways = environmentEquipment.stream()
                .filter(Equipment::isEdgeGateway)
                .sorted(Comparator.comparing(Equipment::getId))
                .map(gateway -> new EdgeGatewayCoverageResource.Gateway(gateway.getId(), gateway.getName(),
                        gateway.getSensorExternalId() == null ? null : gateway.getSensorExternalId().value(),
                        allocation.deviceIdsOf(gateway.getId()).size()))
                .toList();
        int covered = gateways.stream().mapToInt(EdgeGatewayCoverageResource.Gateway::deviceCount).sum();
        int uncovered = allocation.uncoveredDeviceIds().size();
        return new EdgeGatewayCoverageResource(EdgeGatewayAllocation.MAX_DEVICES_PER_EDGE_GATEWAY,
                covered + uncovered, uncovered, gateways);
    }
}
