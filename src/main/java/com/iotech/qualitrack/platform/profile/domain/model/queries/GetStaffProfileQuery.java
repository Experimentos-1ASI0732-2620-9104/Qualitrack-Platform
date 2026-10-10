package com.iotech.qualitrack.platform.profile.domain.model.queries;

/**
 * Query for the profile of a staff member of a laboratory, through the account the staff member signs in with.
 *
 * @param laboratoryId the laboratory
 * @param staffId the staff member
 */
public record GetStaffProfileQuery(Long laboratoryId, Long staffId) {
    public GetStaffProfileQuery {
        if (laboratoryId == null || laboratoryId <= 0) throw new IllegalArgumentException("laboratoryId cannot be null or less than 1");
        if (staffId == null || staffId <= 0) throw new IllegalArgumentException("staffId cannot be null or less than 1");
    }
}
