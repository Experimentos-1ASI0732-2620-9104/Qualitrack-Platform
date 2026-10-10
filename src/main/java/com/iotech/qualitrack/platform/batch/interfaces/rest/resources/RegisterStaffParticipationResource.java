package com.iotech.qualitrack.platform.batch.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Request body to associate a staff member with the batch in the path.
 */
@Schema(name = "RegisterStaffParticipationRequest", description = "Staff member of the laboratory who took part in the batch")
public record RegisterStaffParticipationResource(
        @Schema(description = "Staff member identifier", example = "6") @NotNull @Positive Long staffId
) {
}
