package com.iotech.qualitrack.platform.equipment.application.acl;

import com.iotech.qualitrack.platform.equipment.application.queryservices.BpmConfigQueryService;
import com.iotech.qualitrack.platform.equipment.application.queryservices.EquipmentQueryService;
import com.iotech.qualitrack.platform.equipment.application.queryservices.MaintenanceQueryService;
import com.iotech.qualitrack.platform.equipment.domain.model.aggregates.Equipment;
import com.iotech.qualitrack.platform.equipment.domain.model.queries.GetBpmParameterConfigsByEquipmentIdQuery;
import com.iotech.qualitrack.platform.equipment.domain.model.queries.GetEquipmentByEnvironmentIdQuery;
import com.iotech.qualitrack.platform.equipment.domain.model.queries.GetEquipmentByIdQuery;
import com.iotech.qualitrack.platform.equipment.domain.model.queries.GetEquipmentByLabIdQuery;
import com.iotech.qualitrack.platform.equipment.domain.model.queries.GetMaintenanceByEquipmentIdQuery;
import com.iotech.qualitrack.platform.equipment.domain.model.valueobjects.IotDeviceType;
import com.iotech.qualitrack.platform.equipment.domain.services.EdgeGatewayAllocation;
import com.iotech.qualitrack.platform.equipment.interfaces.acl.EquipmentContextFacade;
import com.iotech.qualitrack.platform.equipment.domain.model.queries.GetEquipmentByDeviceIdQuery;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Exposes the equipment of a laboratory to other bounded contexts through the Equipment query services.
 */
@Service
public class EquipmentContextFacadeImpl implements EquipmentContextFacade {

    private final EquipmentQueryService equipmentQueryService;
    private final BpmConfigQueryService bpmConfigQueryService;
    private final MaintenanceQueryService maintenanceQueryService;

    public EquipmentContextFacadeImpl(EquipmentQueryService equipmentQueryService,
                                      BpmConfigQueryService bpmConfigQueryService,
                                      MaintenanceQueryService maintenanceQueryService) {
        this.equipmentQueryService = equipmentQueryService;
        this.bpmConfigQueryService = bpmConfigQueryService;
        this.maintenanceQueryService = maintenanceQueryService;
    }

    @Override
    public Optional<EquipmentReference> findEquipment(Long laboratoryId, Long equipmentId) {
        if (laboratoryId == null || equipmentId == null || equipmentId <= 0) return Optional.empty();
        if (laboratoryId <= 0) return Optional.empty();
        return equipmentQueryService.handle(new GetEquipmentByIdQuery(laboratoryId, equipmentId))
                .map(EquipmentContextFacadeImpl::toReference);
    }

    /**
     * Retrieves an Edge Gateway belonging to the specified laboratory.
     *
     * <p>Reuses the existing Equipment query service and verifies
     * that the resulting equipment represents an Edge Gateway.</p>
     *
     * @param laboratoryId owning laboratory identifier
     * @param equipmentId Gateway equipment identifier
     * @return the Gateway reference, or an empty Optional
     */
    @Override
    public Optional<EdgeGatewayReference> findEdgeGateway(
            Long laboratoryId,
            Long equipmentId) {

        if (laboratoryId == null || laboratoryId <= 0
                || equipmentId == null || equipmentId <= 0) {
            return Optional.empty();
        }

        return equipmentQueryService.handle(
                        new GetEquipmentByIdQuery(laboratoryId, equipmentId)
                )
                .filter(EquipmentContextFacadeImpl::isEdgeGateway)
                .map(EquipmentContextFacadeImpl::toEdgeGatewayReference);
    }

    /**
     * Retrieves an Edge Gateway by its unique external identifier.
     *
     * <p>This lookup does not require a laboratory identifier,
     * allowing IAM to resolve the Gateway during authentication.</p>
     *
     * @param sensorExternalId unique Gateway identifier
     * @return the Gateway reference, or an empty Optional
     */
    @Override
    public Optional<EdgeGatewayReference> findEdgeGatewayByExternalId(String sensorExternalId) {

        if (sensorExternalId == null || sensorExternalId.isBlank()) {
            return Optional.empty();
        }

        return equipmentQueryService.handle(new GetEquipmentByDeviceIdQuery(sensorExternalId))
                .filter(EquipmentContextFacadeImpl::isEdgeGateway)
                .map(EquipmentContextFacadeImpl::toEdgeGatewayReference);
    }

    /**
     * Tells whether a device is among the share of its environment that the distribution gives to a Gateway, the
     * same share the Gateway reads from its device registry.
     */
    @Override
    public boolean isServedByEdgeGateway(Long laboratoryId, Long environmentId, Long gatewayEquipmentId,
                                         Long deviceId) {
        if (laboratoryId == null || environmentId == null || gatewayEquipmentId == null || deviceId == null) {
            return false;
        }
        var environmentEquipment = equipmentQueryService.handle(
                new GetEquipmentByEnvironmentIdQuery(laboratoryId, environmentId, false)).orElseGet(List::of);
        return EdgeGatewayAllocation.ofEnvironment(environmentEquipment).deviceIdsOf(gatewayEquipmentId)
                .contains(deviceId);
    }

    /**
     * Converts an Equipment aggregate into an Edge Gateway reference.
     *
     * <p>Exposes only the information required by other bounded
     * contexts without leaking the Equipment aggregate.</p>
     *
     * @param equipment validated Edge Gateway aggregate
     * @return immutable Edge Gateway reference
     */
    private static EdgeGatewayReference toEdgeGatewayReference(Equipment equipment) {

        return new EdgeGatewayReference(
                equipment.getId(),
                equipment.getLabId(),
                equipment.getEnvironmentId(),
                equipment.getSensorExternalId().value(),
                equipment.getStatus() == null ? null : equipment.getStatus().name());
    }

    @Override
    public List<EquipmentReference> findLaboratoryEquipment(Long laboratoryId) {
        if (laboratoryId == null || laboratoryId <= 0) return List.of();
        return equipmentQueryService.handle(new GetEquipmentByLabIdQuery(laboratoryId)).stream()
                .map(EquipmentContextFacadeImpl::toReference).toList();
    }

    @Override
    public Optional<EquipmentLogReference> findEquipmentLog(Long laboratoryId, Long environmentId, Long equipmentId) {
        if (laboratoryId == null || laboratoryId <= 0 || environmentId == null || environmentId <= 0
                || equipmentId == null || equipmentId <= 0) {
            return Optional.empty();
        }
        return equipmentQueryService.handle(new GetEquipmentByIdQuery(laboratoryId, equipmentId))
                .filter(equipment -> equipment.isLocatedIn(laboratoryId, environmentId))
                .map(equipment -> new EquipmentLogReference(equipment.getId(), equipment.getLabId(),
                        equipment.getEnvironmentId(), equipment.getName(),
                        equipment.getType() == null ? null : equipment.getType().name(), equipment.getModel(),
                        equipment.getSerialNumber(), equipment.getStatus() == null ? null : equipment.getStatus().name(),
                        equipment.getSensorExternalId() == null ? null : equipment.getSensorExternalId().value(),
                        bpmConfigQueryService.handle(new GetBpmParameterConfigsByEquipmentIdQuery(equipment.getId()))
                                .stream().map(config -> new ParameterRange(config.getParameterName().name(),
                                        config.getMinValue(), config.getMaxValue(), config.getUnit())).toList(),
                        maintenanceQueryService.handle(new GetMaintenanceByEquipmentIdQuery(laboratoryId, environmentId,
                                        equipment.getId())).orElse(List.of())
                                .stream().map(record -> new MaintenanceEntry(record.getId(), record.getMaintenanceDate(),
                                        record.getType() == null ? null : record.getType().name(),
                                        record.getTechnicianName(), record.getDescription())).toList()));
    }

    private static EquipmentReference toReference(Equipment equipment) {
        return new EquipmentReference(equipment.getId(), equipment.getLabId(), equipment.getName(),
                equipment.getSerialNumber(), equipment.getStatus() == null ? null : equipment.getStatus().name());
    }

    /**
     * Determines whether equipment represents an Edge Gateway.
     *
     * <p>Gateways must have the EDGE_GATEWAY category and
     * an external identifier. They must not be classified
     * as environmental devices or container monitors.</p>
     *
     * @param equipment equipment to validate
     * @return true when the equipment represents an Edge Gateway
     */
    private static boolean isEdgeGateway(Equipment equipment) {

        return equipment.getType() != null
                && equipment.isEdgeGateway()
                && !equipment.isIotDevice()
                && equipment.getSensorExternalId() != null;
    }

    @Override
    public Optional<DeviceReference> findDevice(Long laboratoryId, Long environmentId, Long deviceId) {
        if (laboratoryId == null || laboratoryId <= 0 || environmentId == null || deviceId == null || deviceId <= 0) {
            return Optional.empty();
        }
        return equipmentQueryService.handle(new GetEquipmentByIdQuery(laboratoryId, deviceId))
                .filter(equipment -> equipment.isIotDevice() && equipment.isLocatedIn(laboratoryId, environmentId))
                .map(device -> new DeviceReference(device.getId(), device.getLabId(), device.getEnvironmentId(),
                        device.getName(), device.getDeviceType().name(),
                        device.getSensorExternalId() == null ? null : device.getSensorExternalId().value()));
    }

    @Override
    public Optional<DeviceReference> findEnvironmentalDevice(Long laboratoryId, Long environmentId) {
        if (laboratoryId == null || laboratoryId <= 0 || environmentId == null || environmentId <= 0) {
            return Optional.empty();
        }
        return equipmentQueryService.handle(new GetEquipmentByLabIdQuery(laboratoryId)).stream()
                .filter(equipment -> equipment.getDeviceType() == IotDeviceType.ENVIRONMENTAL_DEVICE
                        && equipment.isLocatedIn(laboratoryId, environmentId))
                .findFirst()
                .map(device -> new DeviceReference(device.getId(), device.getLabId(), device.getEnvironmentId(),
                        device.getName(), device.getDeviceType().name(),
                        device.getSensorExternalId() == null ? null : device.getSensorExternalId().value()));
    }

    @Override
    public List<DeviceReference> findDevices(Long laboratoryId) {
        if (laboratoryId == null || laboratoryId <= 0) return List.of();
        return equipmentQueryService.handle(new GetEquipmentByLabIdQuery(laboratoryId)).stream()
                .filter(equipment -> equipment.isIotDevice() && laboratoryId.equals(equipment.getLabId()))
                .map(device -> new DeviceReference(device.getId(), device.getLabId(), device.getEnvironmentId(),
                        device.getName(), device.getDeviceType().name(),
                        device.getSensorExternalId() == null ? null : device.getSensorExternalId().value()))
                .toList();
    }

    @Override
    public Optional<ContainerReference> findContainerMonitor(Long laboratoryId, Long deviceId) {
        if (laboratoryId == null || laboratoryId <= 0 || deviceId == null || deviceId <= 0) return Optional.empty();
        return equipmentQueryService.handle(new GetEquipmentByIdQuery(laboratoryId, deviceId))
                .filter(equipment -> equipment.getDeviceType() == IotDeviceType.CONTAINER_MONITOR)
                .map(container -> new ContainerReference(container.getId(), container.getLabId(), container.getEnvironmentId(),
                        container.getName(), container.getStatus() == null ? null : container.getStatus().name()));
    }
}
