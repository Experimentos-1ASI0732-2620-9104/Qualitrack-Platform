package com.iotech.qualitrack.platform.profile.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "UpdateProfileRequest", description = "Personal data of the authenticated user; empty optional fields are cleared",
        example = "{\"fullName\": \"María Pérez\", \"dni\": \"45678912\", \"phoneNumber\": \"+51 987 654 321\", \"location\": \"Miraflores, Lima\"}")
public record UpdateProfileResource(
        @Schema(description = "Full name (2 to 120 characters)") @NotBlank @Size(max = 120) String fullName,
        @Schema(description = "DNI: 8 digits; optional") @Size(max = 8) String dni,
        @Schema(description = "Phone number: 6 to 15 digits with an optional + prefix; optional") @Size(max = 30) String phoneNumber,
        @Schema(description = "Location, for example district and city; optional") @Size(max = 120) String location
) {
}
