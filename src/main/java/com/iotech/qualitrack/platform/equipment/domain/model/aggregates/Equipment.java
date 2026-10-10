package com.iotech.qualitrack.platform.equipment.domain.model.aggregates;

import com.iotech.qualitrack.platform.equipment.domain.model.commands.RegisterEquipmentCommand;
import com.iotech.qualitrack.platform.equipment.domain.model.commands.RegisterIotDeviceCommand;
import com.iotech.qualitrack.platform.equipment.domain.model.entities.EquipmentStatusChange;
import com.iotech.qualitrack.platform.equipment.domain.model.valueobjects.DeviceId;
import com.iotech.qualitrack.platform.equipment.domain.model.valueobjects.EquipmentStatus;
import com.iotech.qualitrack.platform.equipment.domain.model.valueobjects.EquipmentType;
import com.iotech.qualitrack.platform.equipment.domain.model.valueobjects.IotDeviceType;
import com.iotech.qualitrack.platform.shared.domain.model.aggregates.AbstractDomainAggregateRoot;
import lombok.Getter;

import java.time.Instant;
import java.util.Objects;

/**
 * The Equipment Aggregate Root.
 *
 * <p>Represents a physical equipment registered within a laboratory in the QualiTrack platform.
 * Governs its identity, the environment where it is located and its operational status. An IoT device
 * (environmental device or container monitor) is an equipment with a device type and a device identity.</p>
 */
@Getter
public class Equipment extends AbstractDomainAggregateRoot<Equipment> {

    /**
     * The unique numeric identifier for the equipment.
     */
    private Long id;

    /**
     * The numeric identifier of the laboratory that owns the equipment.
     */
    private Long labId;

    /**
     * Environment where the equipment is located; null until it is associated with one (US47).
     */
    private Long environmentId;

    private String name;

    /**
     * The category of the equipment, modeled as a Value Object.
     */
    private EquipmentType type;

    private String model;
    private String serialNumber;

    private EquipmentStatus status;

    /**
     * External identifier with which Edge recognises the device (if any), modeled as a Value Object.
     */
    private DeviceId sensorExternalId;

    /**
     * Kind of IoT device; null for equipment that produces no telemetry.
     */
    private IotDeviceType deviceType;

    /**
     * Firmware version reported for an IoT device (optional).
     */
    private String firmwareVersion;

    /**
     * Default constructor.
     * Required by the persistence and mapping layers (Assemblers) to reconstruct
     * the entity from the database without triggering business logic.
     */
    public Equipment() {
        // Required for reconstruction by JPA or Assemblers
    }

    /**
     * Reconstructs an Equipment entity from persistence data.
     *
     * @param id The numeric ID.
     * @param labId The laboratory ID.
     * @param environmentId The environment where it is located (can be null).
     * @param name The equipment name.
     * @param type The equipment category (VO).
     * @param model The technical model.
     * @param serialNumber The unique serial number.
     * @param status The operational status.
     * @param sensorExternalId The device identity (VO) (can be null).
     * @param deviceType The IoT device type (can be null).
     * @param firmwareVersion The firmware version (can be null).
     */
    public Equipment(Long id, Long labId, Long environmentId, String name, EquipmentType type, String model,
                     String serialNumber, EquipmentStatus status, DeviceId sensorExternalId,
                     IotDeviceType deviceType, String firmwareVersion) {
        this.id = id;
        this.labId = labId;
        this.environmentId = environmentId;
        this.name = name;
        this.type = type;
        this.model = model;
        this.serialNumber = serialNumber;
        this.status = status;
        this.sensorExternalId = sensorExternalId;
        this.deviceType = deviceType;
        this.firmwareVersion = firmwareVersion;
    }

    /**
     * Registers a new Equipment based on the provided command.
     * <p>Initializes the entity, instantiates the required Value Objects,
     * and sets the default status to OPERATIONAL.</p>
     *
     * @param command The command containing the registration data.
     */
    public Equipment(RegisterEquipmentCommand command) {
        this.labId = Objects.requireNonNull(command.labId(), "Laboratory ID is required");
        this.name = Objects.requireNonNull(command.name(), "Equipment name is required");

        // Instanciamos el Value Object pasando el string del command
        this.type = new EquipmentType(command.type());

        this.model = Objects.requireNonNull(command.model(), "Model is required");
        this.serialNumber = Objects.requireNonNull(command.serialNumber(), "Serial number is required");
        this.status = EquipmentStatus.OPERATIONAL;
    }

    /**
     * Registers an ESP32 environmental device or container monitor with its device identity.
     *
     * @param command The command containing the device registration data.
     */
    public Equipment(RegisterIotDeviceCommand command) {
        this.labId = command.laboratoryId();
        this.name = command.name();
        this.deviceType = command.deviceType();
        this.type = new EquipmentType(command.deviceType().name());
        this.model = command.model();
        this.serialNumber = command.serialNumber();
        this.sensorExternalId = new DeviceId(command.sensorExternalId());
        this.firmwareVersion = command.firmwareVersion();
        this.status = EquipmentStatus.OPERATIONAL;
    }

    /**
     * Whether this equipment is an IoT device that communicates with Edge.
     */
    public boolean isIotDevice() {
        return deviceType != null;
    }

    public boolean isDeviceOfType(IotDeviceType expected) {
        return expected != null && expected == deviceType;
    }

    public boolean belongsTo(Long laboratoryId) {
        return Objects.equals(labId, laboratoryId);
    }

    /**
     * Whether the equipment belongs to the laboratory and is located in the environment.
     */
    public boolean isLocatedIn(Long laboratoryId, Long environmentId) {
        return belongsTo(laboratoryId) && environmentId != null && environmentId.equals(this.environmentId);
    }

    /**
     * Records the environment where the equipment is located (US47, US52, US54).
     *
     * @param environmentId An environment of the same laboratory.
     */
    public void assignToEnvironment(Long environmentId) {
        if (environmentId == null || environmentId <= 0) {
            throw new IllegalArgumentException("Environment id must be a positive number");
        }
        this.environmentId = environmentId;
    }

    /**
     * Links an external IoT sensor to this equipment.
     *
     * @param sensorExternalId The external sensor identifier string.
     */
    public void linkSensor(String sensorExternalId) {
        // El constructor del DeviceId se encarga de las validaciones de negocio (nulos, vacíos, longitud)
        this.sensorExternalId = new DeviceId(sensorExternalId);
    }

    /**
     * Updates the operational status of the equipment.
     *
     * @param newStatus The new operational status.
     */
    public void updateStatus(EquipmentStatus newStatus) {
        this.status = Objects.requireNonNull(newStatus, "New status cannot be null");
    }

    /**
     * Changes the operational status and returns the traceable record of the change (US48).
     * The requested status must differ from the current one; otherwise the current status is kept.
     *
     * @param newStatus Requested operational status.
     * @param reason Optional reason for the change.
     * @param changedByUserId User who registers the change.
     * @param changedAt Moment of the change.
     * @return The status change to keep in the equipment history.
     */
    public EquipmentStatusChange changeStatus(EquipmentStatus newStatus, String reason, Long changedByUserId, Instant changedAt) {
        if (newStatus == null) throw new IllegalArgumentException("Status is required");
        if (newStatus == status) throw new IllegalArgumentException("Equipment is already " + status);
        var previousStatus = status;
        this.status = newStatus;
        return new EquipmentStatusChange(null, id, environmentId, previousStatus, newStatus, reason, changedByUserId, changedAt);
    }
}
