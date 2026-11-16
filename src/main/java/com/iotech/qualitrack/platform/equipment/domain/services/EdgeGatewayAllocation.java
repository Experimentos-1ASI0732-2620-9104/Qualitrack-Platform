package com.iotech.qualitrack.platform.equipment.domain.services;

import com.iotech.qualitrack.platform.equipment.domain.model.aggregates.Equipment;
import com.iotech.qualitrack.platform.equipment.domain.model.valueobjects.EquipmentStatus;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Distribution of the IoT devices of one environment among its Edge Gateways.
 *
 * <p>The user only places devices and Gateways in an environment; which Gateway serves each device is worked out
 * here. Gateways and devices are taken by ascending identifier, and every Gateway receives the next
 * {@value #MAX_DEVICES_PER_EDGE_GATEWAY} devices, like a queue: with two Gateways and 20 devices the first one
 * serves 15 and the second one 5. The devices left over when the Gateways are full have no Gateway.</p>
 *
 * <p>The distribution is calculated, not stored: when a device leaves the environment, the devices after it move
 * up one position and may change Gateway, so they enroll again in their new Gateway.</p>
 *
 * @param deviceIdsByGatewayId devices served by each Gateway, in Gateway order
 * @param uncoveredDeviceIds devices of the environment that no Gateway can serve
 */
public record EdgeGatewayAllocation(Map<Long, List<Long>> deviceIdsByGatewayId, List<Long> uncoveredDeviceIds) {

    /** Number of IoT devices one Edge Gateway serves at most. */
    public static final int MAX_DEVICES_PER_EDGE_GATEWAY = 15;

    /**
     * Distributes devices among Gateways by ascending identifier.
     *
     * @param gatewayIds identifiers of the Edge Gateways of the environment
     * @param deviceIds identifiers of the IoT devices of the environment
     * @return the devices of each Gateway and the devices without one
     */
    public static EdgeGatewayAllocation of(Collection<Long> gatewayIds, Collection<Long> deviceIds) {
        var gateways = gatewayIds.stream().distinct().sorted().toList();
        var devices = deviceIds.stream().distinct().sorted().toList();
        var assigned = new LinkedHashMap<Long, List<Long>>();
        for (int position = 0; position < gateways.size(); position++) {
            int from = Math.min(position * MAX_DEVICES_PER_EDGE_GATEWAY, devices.size());
            int to = Math.min(from + MAX_DEVICES_PER_EDGE_GATEWAY, devices.size());
            assigned.put(gateways.get(position), List.copyOf(devices.subList(from, to)));
        }
        int covered = Math.min(gateways.size() * MAX_DEVICES_PER_EDGE_GATEWAY, devices.size());
        return new EdgeGatewayAllocation(Collections.unmodifiableMap(assigned),
                List.copyOf(devices.subList(covered, devices.size())));
    }

    /**
     * Distributes the IoT devices located in an environment among the operational Edge Gateways located in it.
     *
     * <p>Ordinary equipment is ignored, and so are the devices without an external identifier, which the Edge
     * cannot recognize. A Gateway in maintenance or out of service cannot connect, so it serves no device: its
     * devices go to the next operational Gateway, or are left without one.</p>
     *
     * @param environmentEquipment everything located in one environment
     * @return the devices of each operational Gateway and the devices without one
     */
    public static EdgeGatewayAllocation ofEnvironment(Collection<Equipment> environmentEquipment) {
        return of(environmentEquipment.stream()
                        .filter(equipment -> equipment.isEdgeGateway()
                                && equipment.getStatus() == EquipmentStatus.OPERATIONAL)
                        .map(Equipment::getId).toList(),
                environmentEquipment.stream()
                        .filter(equipment -> equipment.isIotDevice() && equipment.getSensorExternalId() != null)
                        .map(Equipment::getId).toList());
    }

    /**
     * Devices served by one Gateway.
     *
     * @param gatewayId equipment identifier of the Edge Gateway
     * @return its devices, or none when the Gateway is not part of the distribution
     */
    public List<Long> deviceIdsOf(Long gatewayId) {
        return deviceIdsByGatewayId.getOrDefault(gatewayId, List.of());
    }
}
