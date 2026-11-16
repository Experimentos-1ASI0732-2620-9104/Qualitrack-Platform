package com.iotech.qualitrack.platform.equipment.interfaces.acl;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Boundary through which other bounded contexts read the equipment of a laboratory.
 */
public interface EquipmentContextFacade {

    /**
     * Finds an equipment registered in the laboratory.
     *
     * @param laboratoryId the laboratory that must own the equipment
     * @param equipmentId the equipment identifier
     * @return the equipment, or empty when it does not exist in the laboratory
     */
    Optional<EquipmentReference> findEquipment(Long laboratoryId, Long equipmentId);

    /**
     * Finds an Edge Gateway registered within a laboratory.
     *
     * <p>Only equipment classified as EDGE_GATEWAY with
     * a valid external identifier is returned.</p>
     *
     * @param laboratoryId laboratory that owns the Gateway
     * @param equipmentId Gateway equipment identifier
     * @return the Gateway reference, or empty when not found
     *         or when the equipment is not an Edge Gateway
     */
    Optional<EdgeGatewayReference> findEdgeGateway(
            Long laboratoryId,
            Long equipmentId
    );

    /**
     * Finds an Edge Gateway using its unique external identifier.
     *
     * <p>This operation is intended for machine authentication
     * without requiring the Gateway to provide its laboratory
     * or numeric equipment identifier.</p>
     *
     * @param sensorExternalId unique external Gateway identifier
     * @return the Gateway reference, or empty when not found
     *         or when the equipment is not an Edge Gateway
     */
    Optional<EdgeGatewayReference> findEdgeGatewayByExternalId(
            String sensorExternalId
    );

    /**
     * Whether an Edge Gateway serves an IoT device of its environment.
     *
     * <p>The devices of an environment are distributed among its operational Gateways, 15 at most each, so a Gateway
     * serves only its share of them, the same devices it reads from its device registry.</p>
     *
     * @param laboratoryId the laboratory of the Gateway
     * @param environmentId the environment where the Gateway is located
     * @param gatewayEquipmentId the equipment identifier of the Gateway
     * @param deviceId the device (equipment) identifier
     * @return true when the device is among the ones the Gateway serves
     */
    boolean isServedByEdgeGateway(Long laboratoryId, Long environmentId, Long gatewayEquipmentId, Long deviceId);

    /**
     * Finds an IoT device (environmental device or container monitor) located in an environment.
     *
     * @param laboratoryId the laboratory that must own the device
     * @param environmentId the environment where the device must be located
     * @param deviceId the device (equipment) identifier
     * @return the device, or empty when it is not an IoT device located in the environment
     */
    Optional<DeviceReference> findDevice(Long laboratoryId, Long environmentId, Long deviceId);

    /**
     * Finds the environmental device located in an environment; an environment has at most one.
     *
     * @param laboratoryId the laboratory that owns the environment
     * @param environmentId the environment supervised by the device
     * @return the environmental device, or empty when the environment has none
     */
    Optional<DeviceReference> findEnvironmentalDevice(Long laboratoryId, Long environmentId);

    /**
     * Finds a container monitor of the laboratory, which represents the monitored container where lots are stored
     * (US43, US78).
     *
     * @param laboratoryId the laboratory that must own the container monitor
     * @param deviceId the container monitor (equipment) identifier
     * @return the container, or empty when the device is not a container monitor of the laboratory
     */
    Optional<ContainerReference> findContainerMonitor(Long laboratoryId, Long deviceId);

    /**
     * IoT devices (environmental devices and container monitors) of the laboratory, located or not, used to name
     * the devices in indicators and reports (US93-US95).
     *
     * @param laboratoryId the laboratory that owns the devices
     * @return the devices; empty when the laboratory has none
     */
    List<DeviceReference> findDevices(Long laboratoryId);

    /**
     * Equipment of the laboratory, located or not, used by the operational counts of the indicators (US93).
     *
     * @param laboratoryId the laboratory that owns the equipment
     * @return the equipment; empty when the laboratory has none
     */
    List<EquipmentReference> findLaboratoryEquipment(Long laboratoryId);

    /**
     * Finds an equipment located in an environment with its BPM parameter ranges and its maintenance history, used by
     * the equipment log report.
     *
     * @param laboratoryId the laboratory that must own the equipment
     * @param environmentId the environment where the equipment must be located
     * @param equipmentId the equipment identifier
     * @return the equipment, or empty when it is not located in the environment of the laboratory
     */
    Optional<EquipmentLogReference> findEquipmentLog(Long laboratoryId, Long environmentId, Long equipmentId);

    /**
     * Equipment data shared with other bounded contexts.
     *
     * @param id the equipment identifier
     * @param laboratoryId the owning laboratory
     * @param name the equipment name
     * @param serialNumber the serial number
     * @param status the operational status name (OPERATIONAL, MAINTENANCE, OUT_OF_SERVICE or INACTIVE)
     */
    record EquipmentReference(Long id, Long laboratoryId, String name, String serialNumber, String status) {
        /**
         * Only operational equipment can be used in a manufacturing process.
         *
         * @return true when the equipment is operational
         */
        public boolean isAvailable() {
            return "OPERATIONAL".equals(status);
        }
    }

    /**
     * Edge Gateway information exposed to other bounded contexts.
     *
     * <p>This reference contains the information required
     * to identify a Gateway and determine its current
     * laboratory, environment and operational status.</p>
     *
     * <p>Authentication credentials are not included because
     * they are managed exclusively by IAM.</p>
     *
     * @param equipmentId Gateway equipment identifier
     * @param laboratoryId owning laboratory identifier
     * @param environmentId assigned environment, or null if unassigned
     * @param sensorExternalId unique external Gateway identifier
     * @param status current operational status
     */
    record EdgeGatewayReference(Long equipmentId, Long laboratoryId, Long environmentId, String sensorExternalId, String status) {

        /**
         * Determines whether the Gateway is operational.
         *
         * @return true when the Gateway is operational
         */
        public boolean isOperational() {return "OPERATIONAL".equals(status);}

        /**
         * Determines whether the Gateway has an assigned environment.
         *
         * @return true when an environment is assigned
         */
        public boolean isAssigned() {return environmentId != null;}
    }

    /**
     * IoT device data shared with other bounded contexts.
     *
     * @param id the device (equipment) identifier
     * @param laboratoryId the owning laboratory
     * @param environmentId the environment where the device is located
     * @param name the device name
     * @param deviceType ENVIRONMENTAL_DEVICE or CONTAINER_MONITOR
     * @param sensorExternalId the identifier with which Edge recognises the device
     */
    record DeviceReference(Long id, Long laboratoryId, Long environmentId, String name, String deviceType,
                           String sensorExternalId) {
    }

    /**
     * Equipment with its BPM parameter ranges and maintenance history, shared with other bounded contexts.
     *
     * @param type the equipment type name
     * @param status the operational status name (OPERATIONAL, MAINTENANCE, OUT_OF_SERVICE or INACTIVE)
     * @param sensorExternalId the identifier with which Edge recognises the device, or null
     * @param parameters the configured BPM parameter ranges
     * @param maintenance the maintenance records, in any order
     */
    record EquipmentLogReference(Long id, Long laboratoryId, Long environmentId, String name, String type, String model,
                                 String serialNumber, String status, String sensorExternalId,
                                 List<ParameterRange> parameters, List<MaintenanceEntry> maintenance) {
        public EquipmentLogReference {
            parameters = List.copyOf(parameters);
            maintenance = List.copyOf(maintenance);
        }
    }

    /** BPM range configured for a parameter of an equipment. */
    record ParameterRange(String parameterName, Double minValue, Double maxValue, String unit) {
    }

    /**
     * Maintenance record of an equipment.
     *
     * @param type the maintenance type name
     */
    record MaintenanceEntry(Long id, LocalDate performedOn, String type, String technicianName, String description) {
    }

    /**
     * Monitored container shared with other bounded contexts.
     *
     * @param id the container monitor (equipment) identifier
     * @param laboratoryId the owning laboratory
     * @param environmentId the environment where the container is located, or null when it is not located yet
     * @param name the container monitor name
     * @param status the operational status name (OPERATIONAL, MAINTENANCE, OUT_OF_SERVICE or INACTIVE)
     */
    record ContainerReference(Long id, Long laboratoryId, Long environmentId, String name, String status) {
        /**
         * Only an operational container monitor can receive lots.
         *
         * @return true when the container monitor is operational
         */
        public boolean isAvailable() {
            return "OPERATIONAL".equals(status);
        }

        /**
         * @return true when the container is located in an environment
         */
        public boolean isLocated() {
            return environmentId != null;
        }
    }
}
