package com.iotech.qualitrack.platform.equipment.application.internal.queryservices;

import com.iotech.qualitrack.platform.equipment.application.queryservices.BpmConfigQueryService;
import com.iotech.qualitrack.platform.equipment.domain.model.entities.BpmParameterConfig;
import com.iotech.qualitrack.platform.equipment.domain.model.queries.GetBpmParameterConfigQuery;
import com.iotech.qualitrack.platform.equipment.domain.model.queries.GetBpmParameterConfigsByEquipmentIdQuery;
import com.iotech.qualitrack.platform.equipment.domain.repositories.BpmParameterConfigRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Application service implementation that resolves BPM parameter configuration read queries.
 */
@Service
public class BpmConfigQueryServiceImpl implements BpmConfigQueryService {

    private final BpmParameterConfigRepository bpmConfigRepository;

    public BpmConfigQueryServiceImpl(BpmParameterConfigRepository bpmConfigRepository) {
        this.bpmConfigRepository = bpmConfigRepository;
    }

    @Override
    public List<BpmParameterConfig> handle(GetBpmParameterConfigsByEquipmentIdQuery query) {
        return bpmConfigRepository.findAllByEquipmentId(query.equipmentId());
    }

    @Override
    public Optional<BpmParameterConfig> handle(GetBpmParameterConfigQuery query) {
        return bpmConfigRepository.findByEquipmentIdAndParameterName(query.equipmentId(), query.parameterName());
    }
}