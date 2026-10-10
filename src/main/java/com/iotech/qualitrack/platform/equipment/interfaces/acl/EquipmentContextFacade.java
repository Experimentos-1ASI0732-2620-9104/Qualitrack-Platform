package com.iotech.qualitrack.platform.equipment.interfaces.acl;

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
