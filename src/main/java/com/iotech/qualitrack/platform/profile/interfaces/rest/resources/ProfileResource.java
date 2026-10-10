package com.iotech.qualitrack.platform.profile.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "Profile", description = "Personal data of a person with the account it belongs to")
public record ProfileResource(
        @Schema(description = "Account of the profile", example = "4") Long userId,
        @Schema(description = "Staff record of the person in the laboratory; null for the quality manager", example = "2") Long staffId,
        @Schema(description = "Username used to sign in", example = "maria@labsur.pe") String username,
        @Schema(description = "E-mail of the account", example = "maria@labsur.pe") String email,
        @Schema(description = "Role names of the account", example = "[\"ROLE_LAB_OPERATOR\"]") List<String> roles,
        @Schema(description = "Full name; null until the person completes the profile", example = "María Pérez") String fullName,
        @Schema(description = "DNI (8 digits)", example = "45678912") String dni,
        @Schema(description = "Contact phone number", example = "+51 987 654 321") String phoneNumber,
        @Schema(description = "Location", example = "Miraflores, Lima") String location,
        @Schema(description = "Position registered by the quality manager for a staff member", example = "Analista de control de calidad") String position,
        @Schema(description = "Whether the profile has a photo") boolean hasPhoto,
        @Schema(description = "When the photo was uploaded (ISO-8601)") String photoUpdatedAt,
        @Schema(description = "Last change of the profile (ISO-8601); null when it was never saved") String updatedAt
) {
}
