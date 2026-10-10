package com.iotech.qualitrack.platform.profile.domain.model.commands;

import com.iotech.qualitrack.platform.profile.domain.model.valueobjects.Dni;
import com.iotech.qualitrack.platform.profile.domain.model.valueobjects.Location;
import com.iotech.qualitrack.platform.profile.domain.model.valueobjects.PersonName;
import com.iotech.qualitrack.platform.profile.domain.model.valueobjects.PhoneNumber;

/**
 * Command to replace the personal data of the profile of the authenticated user.
 *
 * @param userId the account of the profile
 * @param fullName full name (required)
 * @param dni DNI, or null to leave it empty
 * @param phoneNumber phone number, or null to leave it empty
 * @param location location, or null to leave it empty
 */
public record UpdateProfileCommand(Long userId, PersonName fullName, Dni dni, PhoneNumber phoneNumber, Location location) {
    public UpdateProfileCommand {
        if (userId == null || userId <= 0) throw new IllegalArgumentException("userId cannot be null or less than 1");
        if (fullName == null) throw new IllegalArgumentException("The full name is required");
    }

    /**
     * Builds the command from the values typed by the person; blank optional values clear the field.
     *
     * @throws IllegalArgumentException when a value is not valid
     */
    public static UpdateProfileCommand of(Long userId, String fullName, String dni, String phoneNumber, String location) {
        return new UpdateProfileCommand(userId, new PersonName(fullName),
                blank(dni) ? null : new Dni(dni),
                blank(phoneNumber) ? null : new PhoneNumber(phoneNumber),
                blank(location) ? null : new Location(location));
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
