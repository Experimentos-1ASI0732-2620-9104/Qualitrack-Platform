package com.iotech.qualitrack.platform.laboratory.domain.model.valueobjects;

/**
 * What a staff member can do in the platform with the account created when the quality manager registers them.
 */
public enum StaffAccessRole {
    /** Operative staff: consults the laboratory and registers the operations assigned to them. */
    OPERATOR,
    /** Auditor: consults records, traceability, audit and reports without registering or changing anything. */
    AUDITOR
}
