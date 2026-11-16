package com.iotech.qualitrack.platform.iam.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response containing a newly provisioned Edge Gateway API key.
 *
 * <p>The API key is returned only once during provisioning.
 * It cannot be retrieved through subsequent requests.</p>
 *
 * @param sensorExternalId public Gateway identifier
 * @param apiKey generated authentication credential
 */
@Schema(name = "IssuedEdgeGatewayApiKey", description = "One-time Edge Gateway API key provisioning response")
public record IssuedEdgeGatewayApiKeyResource(

        @Schema(description = "Unique external Gateway identifier", example = "EDGE-001")
        String sensorExternalId,

        @Schema(description = "Generated API key. Returned only once.", accessMode = Schema.AccessMode.READ_ONLY)
        String apiKey
) {}