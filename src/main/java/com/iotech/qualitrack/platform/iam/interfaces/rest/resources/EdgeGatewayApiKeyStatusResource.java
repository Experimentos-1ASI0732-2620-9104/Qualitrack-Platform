package com.iotech.qualitrack.platform.iam.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Whether an Edge Gateway already has an API key.
 *
 * <p>The key itself is never part of this resource: it is shown once, when it is generated.</p>
 *
 * @param issued true when an API key has been generated for the Gateway
 */
@Schema(name = "EdgeGatewayApiKeyStatus", description = "Whether an API key has been generated for the Edge Gateway")
public record EdgeGatewayApiKeyStatusResource(
        @Schema(description = "Whether the Gateway already has an API key", example = "true")
        boolean issued
) {
}
