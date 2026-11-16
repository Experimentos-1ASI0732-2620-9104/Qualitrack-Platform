package com.iotech.qualitrack.platform.equipment.domain.model.queries;

/**
 * Query to get the equipment located in one environment of a laboratory.
 *
 * <p>The Edge of each environment uses it with {@code iotDevicesOnly} to read only its own nodes instead of
 * every device of the laboratory.</p>
 *
 * @param laboratoryId laboratory that owns the environment. Cannot be null or less than 1.
 * @param environmentId environment whose equipment is requested. Cannot be null or less than 1.
 * @param iotDevicesOnly whether to keep only the IoT devices (environmental devices and container monitors)
 */
public record GetEquipmentByEnvironmentIdQuery(Long laboratoryId, Long environmentId, boolean iotDevicesOnly) {
    public GetEquipmentByEnvironmentIdQuery {
        if (laboratoryId == null || laboratoryId <= 0) {
            throw new IllegalArgumentException("Laboratory id is required and must be greater than 0.");
        }
        if (environmentId == null || environmentId <= 0) {
            throw new IllegalArgumentException("Environment id is required and must be greater than 0.");
        }
    }
}
