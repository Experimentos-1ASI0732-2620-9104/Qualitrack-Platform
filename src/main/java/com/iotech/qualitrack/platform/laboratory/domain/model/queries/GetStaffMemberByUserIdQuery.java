package com.iotech.qualitrack.platform.laboratory.domain.model.queries;

/**
 * Query for the staff member that signs in with an account.
 *
 * @param userId the sign-in account
 */
public record GetStaffMemberByUserIdQuery(Long userId) {
    public GetStaffMemberByUserIdQuery {
        if (userId == null || userId <= 0) throw new IllegalArgumentException("userId must be a positive number");
    }
}
