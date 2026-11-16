package com.iotech.qualitrack.platform.equipment.domain.model.valueobjects;

/**
 * Operational status of an equipment. {@code MAINTENANCE} and {@code OUT_OF_SERVICE} mark the equipment that
 * needs attention, for example in the calibration alerts.
 */
public enum EquipmentStatus {
    OPERATIONAL,
    MAINTENANCE,
    OUT_OF_SERVICE,
    INACTIVE
}