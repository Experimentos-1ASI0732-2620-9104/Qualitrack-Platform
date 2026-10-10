package com.iotech.qualitrack.platform.laboratory.domain.model.queries;

/**
 * Query for a staff member of a laboratory.
 *
 * @param laboratoryId laboratory that must own the staff member
 * @param staffId staff member identifier
 */
public record GetStaffMemberByIdQuery(Long laboratoryId, Long staffId) {
    public GetStaffMemberByIdQuery {
        if (laboratoryId == null || laboratoryId <= 0) throw new IllegalArgumentException("laboratoryId must be a positive number");
        if (staffId == null || staffId <= 0) throw new IllegalArgumentException("staffId must be a positive number");
    }
}
