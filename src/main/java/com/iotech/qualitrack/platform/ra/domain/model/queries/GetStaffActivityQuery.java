package com.iotech.qualitrack.platform.ra.domain.model.queries;

/**
 * Query for the operations registered by a staff member of a laboratory (US92).
 *
 * @param laboratoryId laboratory that owns the staff member
 * @param staffId staff member whose activity is requested
 */
public record GetStaffActivityQuery(Long laboratoryId, Long staffId) {
    public GetStaffActivityQuery {
        if (laboratoryId == null || laboratoryId <= 0) throw new IllegalArgumentException("laboratoryId must be a positive number");
        if (staffId == null || staffId <= 0) throw new IllegalArgumentException("staffId must be a positive number");
    }
}
