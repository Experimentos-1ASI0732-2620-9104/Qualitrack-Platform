package com.iotech.qualitrack.platform.laboratory.application.queryservices;

import com.iotech.qualitrack.platform.laboratory.domain.model.aggregates.StaffMember;
import com.iotech.qualitrack.platform.laboratory.domain.model.queries.GetStaffByLabIdQuery;
import com.iotech.qualitrack.platform.laboratory.domain.model.queries.GetStaffMemberByIdQuery;
import com.iotech.qualitrack.platform.laboratory.domain.model.queries.GetStaffMemberByUserIdQuery;

import java.util.List;
import java.util.Optional;

/**
 * Application service contract for staff member read queries.
 */
public interface StaffQueryService {

    /**
     * Handles retrieval of all staff members belonging to a specific laboratory.
     *
     * @param query laboratory-id query
     * @return list of staff members for the given laboratory
     * @see GetStaffByLabIdQuery
     */
    List<StaffMember> handle(GetStaffByLabIdQuery query);

    /**
     * @return the staff member, or empty when it is not registered in the laboratory
     */
    Optional<StaffMember> handle(GetStaffMemberByIdQuery query);

    Optional<StaffMember> handle(GetStaffMemberByUserIdQuery query);
}