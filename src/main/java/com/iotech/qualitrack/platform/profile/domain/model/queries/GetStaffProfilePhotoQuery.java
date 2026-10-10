package com.iotech.qualitrack.platform.profile.domain.model.queries;

/**
 * Query for the photo of a staff member of a laboratory.
 *
 * @param laboratoryId the laboratory
 * @param staffId the staff member
 */
public record GetStaffProfilePhotoQuery(Long laboratoryId, Long staffId) {
    public GetStaffProfilePhotoQuery {
        if (laboratoryId == null || laboratoryId <= 0) throw new IllegalArgumentException("laboratoryId cannot be null or less than 1");
        if (staffId == null || staffId <= 0) throw new IllegalArgumentException("staffId cannot be null or less than 1");
    }
}
