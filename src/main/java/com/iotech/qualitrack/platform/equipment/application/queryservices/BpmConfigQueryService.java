package com.iotech.qualitrack.platform.equipment.application.queryservices;

import com.iotech.qualitrack.platform.equipment.domain.model.entities.BpmParameterConfig;
import com.iotech.qualitrack.platform.equipment.domain.model.queries.GetBpmParameterConfigQuery;
import com.iotech.qualitrack.platform.equipment.domain.model.queries.GetBpmParameterConfigsByEquipmentIdQuery;

import java.util.List;
import java.util.Optional;

/**
 * Application service contract for BPM parameter configuration read queries.
 */
public interface BpmConfigQueryService {

    /**
     * Handles retrieval of all configured BPM parameter ranges (e.g., Temperature, Pressure)
     * for a specific equipment.
     *
     * @param query equipment-id query
     * @return list of parameter configurations for the given equipment
     * @see GetBpmParameterConfigsByEquipmentIdQuery
     */
    List<BpmParameterConfig> handle(GetBpmParameterConfigsByEquipmentIdQuery query);

    /**
     * Handles retrieval of the configured range of one parameter of an equipment.
     *
     * @param query equipment and parameter name
     * @return the configuration, or empty when the parameter is not configured
     */
    Optional<BpmParameterConfig> handle(GetBpmParameterConfigQuery query);
}