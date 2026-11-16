package com.iotech.qualitrack.platform.equipment.domain.services;

import com.iotech.qualitrack.platform.equipment.domain.model.aggregates.Equipment;
import com.iotech.qualitrack.platform.equipment.domain.model.valueobjects.DeviceId;
import com.iotech.qualitrack.platform.equipment.domain.model.valueobjects.EquipmentStatus;
import com.iotech.qualitrack.platform.equipment.domain.model.valueobjects.EquipmentType;
import com.iotech.qualitrack.platform.equipment.domain.model.valueobjects.IotDeviceType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers how the IoT devices of an environment are distributed among its Edge Gateways.
 */
class EdgeGatewayAllocationTests {

    @Test
    void environmentWithoutGatewaysLeavesEveryDeviceUncovered() {
        var allocation = EdgeGatewayAllocation.of(List.of(), devices(1, 3));

        assertThat(allocation.deviceIdsByGatewayId()).isEmpty();
        assertThat(allocation.uncoveredDeviceIds()).containsExactly(1L, 2L, 3L);
        assertThat(allocation.deviceIdsOf(90L)).isEmpty();
    }

    @Test
    void oneGatewayServesUpToFifteenDevices() {
        var allocation = EdgeGatewayAllocation.of(List.of(90L), devices(1, 15));

        assertThat(allocation.deviceIdsOf(90L)).hasSize(15).containsExactlyElementsOf(devices(1, 15));
        assertThat(allocation.uncoveredDeviceIds()).isEmpty();
    }

    @Test
    void devicesBeyondTheCapacityOfTheOnlyGatewayAreUncovered() {
        var allocation = EdgeGatewayAllocation.of(List.of(90L), devices(1, 20));

        assertThat(allocation.deviceIdsOf(90L)).containsExactlyElementsOf(devices(1, 15));
        assertThat(allocation.uncoveredDeviceIds()).containsExactlyElementsOf(devices(16, 20));
    }

    @Test
    void secondGatewayReceivesTheDevicesTheFirstOneCannotServe() {
        var allocation = EdgeGatewayAllocation.of(List.of(91L, 90L), devices(1, 20));

        assertThat(allocation.deviceIdsByGatewayId().keySet()).containsExactly(90L, 91L);
        assertThat(allocation.deviceIdsOf(90L)).containsExactlyElementsOf(devices(1, 15));
        assertThat(allocation.deviceIdsOf(91L)).containsExactlyElementsOf(devices(16, 20));
        assertThat(allocation.uncoveredDeviceIds()).isEmpty();
    }

    @Test
    void gatewayWithoutDevicesLeftStaysInTheDistributionWithNone() {
        var allocation = EdgeGatewayAllocation.of(List.of(90L, 91L), devices(1, 4));

        assertThat(allocation.deviceIdsOf(90L)).hasSize(4);
        assertThat(allocation.deviceIdsOf(91L)).isEmpty();
        assertThat(allocation.uncoveredDeviceIds()).isEmpty();
    }

    @Test
    void devicesAfterARemovedOneMoveUpInTheQueue() {
        var remaining = devices(1, 20).stream().filter(id -> id != 3L).toList();
        var allocation = EdgeGatewayAllocation.of(List.of(90L, 91L), remaining);

        assertThat(allocation.deviceIdsOf(90L)).hasSize(15).contains(16L).doesNotContain(3L);
        assertThat(allocation.deviceIdsOf(91L)).containsExactly(17L, 18L, 19L, 20L);
    }

    @Test
    void gatewayOutOfServiceServesNoDeviceAndTheNextOperationalOneTakesThem() {
        var environment = new ArrayList<Equipment>();
        environment.add(gateway(90L, EquipmentStatus.OUT_OF_SERVICE));
        environment.add(gateway(91L, EquipmentStatus.OPERATIONAL));
        devices(1, 20).forEach(id -> environment.add(device(id)));

        var allocation = EdgeGatewayAllocation.ofEnvironment(environment);

        assertThat(allocation.deviceIdsOf(90L)).isEmpty();
        assertThat(allocation.deviceIdsOf(91L)).containsExactlyElementsOf(devices(1, 15));
        assertThat(allocation.uncoveredDeviceIds()).containsExactlyElementsOf(devices(16, 20));
    }

    @Test
    void onlyGatewayInMaintenanceLeavesEveryDeviceUncovered() {
        var environment = new ArrayList<Equipment>();
        environment.add(gateway(90L, EquipmentStatus.MAINTENANCE));
        devices(1, 3).forEach(id -> environment.add(device(id)));

        var allocation = EdgeGatewayAllocation.ofEnvironment(environment);

        assertThat(allocation.deviceIdsOf(90L)).isEmpty();
        assertThat(allocation.uncoveredDeviceIds()).containsExactly(1L, 2L, 3L);
    }

    private static List<Long> devices(long from, long to) {
        return LongStream.rangeClosed(from, to).boxed().toList();
    }

    private static Equipment gateway(Long id, EquipmentStatus status) {
        return new Equipment(id, 1L, 5L, "Edge Gateway " + id, new EquipmentType(Equipment.EDGE_GATEWAY_TYPE),
                "Raspberry Pi 4", "RPI-" + id, status, new DeviceId("EDGE-" + id), null, null);
    }

    private static Equipment device(Long id) {
        return new Equipment(id, 1L, 5L, "Monitor " + id, new EquipmentType("IOT_DEVICE"), "ESP32-S3", "SN-" + id,
                EquipmentStatus.OPERATIONAL, new DeviceId("ESP32-" + id), IotDeviceType.CONTAINER_MONITOR, "1.0.0");
    }
}
