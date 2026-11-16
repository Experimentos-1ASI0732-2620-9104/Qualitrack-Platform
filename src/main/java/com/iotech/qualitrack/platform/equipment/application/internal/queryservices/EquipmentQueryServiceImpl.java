package com.iotech.qualitrack.platform.equipment.application.internal.queryservices;

import com.iotech.qualitrack.platform.equipment.application.internal.outboundservices.acl.ExternalLabService;
import com.iotech.qualitrack.platform.equipment.application.queryservices.EquipmentQueryService;
import com.iotech.qualitrack.platform.equipment.domain.model.aggregates.Equipment;
import com.iotech.qualitrack.platform.equipment.domain.model.queries.GetCalibrationAlertsByLabIdQuery;
import com.iotech.qualitrack.platform.equipment.domain.model.queries.GetEquipmentByDeviceIdQuery;
import com.iotech.qualitrack.platform.equipment.domain.model.queries.GetEquipmentByEnvironmentIdQuery;
import com.iotech.qualitrack.platform.equipment.domain.model.queries.GetEquipmentByIdQuery;
import com.iotech.qualitrack.platform.equipment.domain.model.queries.GetEquipmentByLabIdQuery;
import com.iotech.qualitrack.platform.equipment.domain.model.valueobjects.DeviceId;
import com.iotech.qualitrack.platform.equipment.domain.model.valueobjects.EquipmentStatus;
import com.iotech.qualitrack.platform.equipment.domain.repositories.EquipmentRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Application service implementation that resolves equipment read queries.
 */
@Service
public class EquipmentQueryServiceImpl implements EquipmentQueryService {

    private final EquipmentRepository equipmentRepository;
    private final ExternalLabService externalLabService;

    /**
     * Creates the service with the equipment repository and the Laboratory ACL.
     *
     * @param equipmentRepository equipment persistence port
     * @param externalLabService checks that an environment belongs to the laboratory
     */
    public EquipmentQueryServiceImpl(EquipmentRepository equipmentRepository, ExternalLabService externalLabService) {
        this.equipmentRepository = equipmentRepository;
        this.externalLabService = externalLabService;
    }

    @Override
    public Optional<Equipment> handle(GetEquipmentByIdQuery query) {
        return equipmentRepository.findById(query.equipmentId())
                .filter(equipment -> equipment.belongsTo(query.laboratoryId()));
    }

    @Override
    public List<Equipment> handle(GetEquipmentByLabIdQuery query) {
        return equipmentRepository.findAllByLabId(query.laboratoryId());
    }

    @Override
    public Optional<List<Equipment>> handle(GetEquipmentByEnvironmentIdQuery query) {
        if (!externalLabService.existsEnvironment(query.laboratoryId(), query.environmentId())) {
            return Optional.empty();
        }
        return Optional.of(equipmentRepository.findAllByLabIdAndEnvironmentId(query.laboratoryId(), query.environmentId())
                .stream()
                .filter(equipment -> !query.iotDevicesOnly() || equipment.isIotDevice())
                .toList());
    }

    @Override
    public Optional<Equipment> handle(GetEquipmentByDeviceIdQuery query) {
        return equipmentRepository.findBySensorExternalId(new DeviceId(query.deviceId()));
    }

    @Override
    public List<Equipment> handle(GetCalibrationAlertsByLabIdQuery query) {
        return equipmentRepository.findAllByLabId(query.laboratoryId())
                .stream()
                .filter(equipment -> equipment.getStatus() == EquipmentStatus.MAINTENANCE ||
                        equipment.getStatus() == EquipmentStatus.OUT_OF_SERVICE)
                .toList();
    }
}