package com.iotech.qualitrack.platform.laboratory.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Staff member registered with the account they use to sign in.
 */
@Schema(name = "RegisteredStaffResponse", description = "Registered staff member and the delivery of their credentials")
public record RegisteredStaffResource(StaffMemberResource staffMember, StaffCredentialsResource credentials) {
}
