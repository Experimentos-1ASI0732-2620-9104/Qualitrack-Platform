package com.iotech.qualitrack.platform.equipment.domain.model.queries;

/**
 * Query to get the configured range of one BPM parameter of an equipment.
 *
 * @param equipmentId   equipment of the configuration
 * @param parameterName parameter configured, for example TEMPERATURE
 */
public record GetBpmParameterConfigQuery(Long equipmentId, String parameterName) {
    public GetBpmParameterConfigQuery {
        if (equipmentId == null || equipmentId <= 0) {
            throw new IllegalArgumentException("Equipment id is required and must be greater than 0.");
        }
        if (parameterName == null || parameterName.isBlank()) {
            throw new IllegalArgumentException("Parameter name is required.");
        }
    }
}
