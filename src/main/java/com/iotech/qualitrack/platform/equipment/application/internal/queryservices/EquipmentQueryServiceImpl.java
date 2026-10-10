package com.iotech.qualitrack.platform.equipment.application.internal.queryservices;

import com.iotech.qualitrack.platform.equipment.application.queryservices.EquipmentQueryService;
import com.iotech.qualitrack.platform.equipment.domain.model.aggregates.Equipment;
import com.iotech.qualitrack.platform.equipment.domain.model.queries.GetCalibrationAlertsByLabIdQuery;
import com.iotech.qualitrack.platform.equipment.domain.model.queries.GetEquipmentByDeviceIdQuery;
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

    public EquipmentQueryServiceImpl(EquipmentRepository equipmentRepository) {
        this.equipmentRepository = equipmentRepository;
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