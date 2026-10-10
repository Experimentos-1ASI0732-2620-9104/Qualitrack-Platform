package com.iotech.qualitrack.platform.equipment.application.internal.queryservices;

import com.iotech.qualitrack.platform.equipment.application.queryservices.MaintenanceQueryService;
import com.iotech.qualitrack.platform.equipment.domain.model.aggregates.MaintenanceRecord;
import com.iotech.qualitrack.platform.equipment.domain.model.queries.GetMaintenanceByEquipmentIdQuery;
import com.iotech.qualitrack.platform.equipment.domain.repositories.EquipmentRepository;
import com.iotech.qualitrack.platform.equipment.domain.repositories.MaintenanceRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Application service implementation that resolves maintenance record read queries.
 */
@Service
public class MaintenanceQueryServiceImpl implements MaintenanceQueryService {

    private final MaintenanceRepository maintenanceRepository;
    private final EquipmentRepository equipmentRepository;

    public MaintenanceQueryServiceImpl(MaintenanceRepository maintenanceRepository, EquipmentRepository equipmentRepository) {
        this.maintenanceRepository = maintenanceRepository;
        this.equipmentRepository = equipmentRepository;
    }

    @Override
    public Optional<List<MaintenanceRecord>> handle(GetMaintenanceByEquipmentIdQuery query) {
        return equipmentRepository.findById(query.equipmentId())
                .filter(equipment -> equipment.isLocatedIn(query.laboratoryId(), query.environmentId()))
                .map(equipment -> maintenanceRepository.findAllByEquipmentId(equipment.getId()));
    }
}
