package com.iotech.qualitrack.platform.equipment.domain.model.valueobjects;

/**
 * Kind of maintenance registered for an equipment (US50, TS36).
 */
public enum MaintenanceType {
    PREVENTIVE,
    CORRECTIVE,
    CALIBRATION,
    INSPECTION,
    OTHER
}