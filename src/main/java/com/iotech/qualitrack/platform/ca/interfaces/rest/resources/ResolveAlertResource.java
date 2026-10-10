package com.iotech.qualitrack.platform.ca.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Resolution of a deviation alert; the authenticated user resolves it.
 *
 * @param resolutionNotes corrective action or resolution notes
 */
@Schema(
        name = "ResolveAlertRequest",
        description = "Request payload for resolving a deviation alert",
        example = "{\"resolutionNotes\": \"Equipment recalibrated and batch quality review completed.\"}"
)
public record ResolveAlertResource(
        @Schema(description = "Corrective action or resolution notes", example = "Equipment recalibrated and batch quality review completed.")
        String resolutionNotes
) {
}
