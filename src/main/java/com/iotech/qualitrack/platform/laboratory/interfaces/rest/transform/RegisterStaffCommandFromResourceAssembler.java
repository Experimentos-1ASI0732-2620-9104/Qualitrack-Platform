package com.iotech.qualitrack.platform.laboratory.interfaces.rest.transform;

import com.iotech.qualitrack.platform.laboratory.domain.model.commands.RegisterStaffCommand;
import com.iotech.qualitrack.platform.laboratory.domain.model.valueobjects.StaffAccessRole;
import com.iotech.qualitrack.platform.laboratory.interfaces.rest.resources.RegisterStaffResource;

import java.util.Arrays;
import java.util.Locale;

/**
 * Assembler to convert a RegisterStaffResource into a RegisterStaffCommand.
 */
public class RegisterStaffCommandFromResourceAssembler {

    /**
     * @throws IllegalArgumentException when the access role is not OPERATOR or AUDITOR
     */
    public static RegisterStaffCommand toCommandFromResource(RegisterStaffResource resource, Long laboratoryId) {
        var requested = resource.accessRole().trim().toUpperCase(Locale.ROOT);
        var accessRole = Arrays.stream(StaffAccessRole.values()).filter(value -> value.name().equals(requested)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("accessRole must be OPERATOR or AUDITOR"));
        return new RegisterStaffCommand(laboratoryId, resource.fullName(), resource.role(), resource.email(), accessRole);
    }
}
