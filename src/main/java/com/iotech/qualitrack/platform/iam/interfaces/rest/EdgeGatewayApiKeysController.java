package com.iotech.qualitrack.platform.iam.interfaces.rest;

import com.iotech.qualitrack.platform.iam.application.internal.commandservices.EdgeGatewayApiKeyService;
import com.iotech.qualitrack.platform.iam.interfaces.rest.resources.EdgeGatewayApiKeyStatusResource;
import com.iotech.qualitrack.platform.iam.interfaces.rest.resources.IssuedEdgeGatewayApiKeyResource;
import com.iotech.qualitrack.platform.shared.interfaces.rest.resources.ErrorResource;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for provisioning Edge Gateway API keys.
 *
 * <p>Only authorized laboratory administrators and quality
 * managers can generate Gateway credentials.</p>
 *
 * <p>API keys are returned only once and must not be cached.</p>
 */
@RestController
@RequestMapping(
        "/api/v1/laboratories/{laboratoryId}/edge-gateways"
)
@PreAuthorize(
        "@tenantAccess.allows('laboratoryId', #laboratoryId) "
                + "and hasAnyAuthority('ROLE_QA_MANAGER', 'ROLE_ADMIN')"
)
public class EdgeGatewayApiKeysController {

    private final EdgeGatewayApiKeyService apiKeyService;

    public EdgeGatewayApiKeysController(
            EdgeGatewayApiKeyService apiKeyService) {

        this.apiKeyService = apiKeyService;
    }

    /**
     * Tells whether an Edge Gateway already has an API key.
     *
     * <p>The key is never returned: it is shown once, when it
     * is generated.</p>
     *
     * @param laboratoryId laboratory identifier
     * @param equipmentId Edge Gateway equipment identifier
     * @return whether an API key has been generated
     */
    @GetMapping("/{equipmentId}/api-key")
    @Operation(
            summary = "Get Edge Gateway API key status",
            description = "Tells whether an API key has already been "
                    + "generated for the Edge Gateway. The API key "
                    + "itself is never returned."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "API key status",
                    content = @Content(
                            schema = @Schema(
                                    implementation =
                                            EdgeGatewayApiKeyStatusResource.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication required"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Insufficient laboratory permissions, "
                            + "or equipment not available to the account"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "The equipment is not an Edge Gateway",
                    content = @Content(
                            schema = @Schema(
                                    implementation = ErrorResource.class
                            )
                    )
            )
    })
    public ResponseEntity<EdgeGatewayApiKeyStatusResource> status(
            @PathVariable Long laboratoryId,
            @PathVariable Long equipmentId) {

        var resource = new EdgeGatewayApiKeyStatusResource(
                apiKeyService.isProvisioned(laboratoryId, equipmentId)
        );

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(resource);
    }

    /**
     * Generates an API key for a registered Edge Gateway.
     *
     * <p>The Gateway must belong to the laboratory,
     * be operational, and have an assigned environment.</p>
     *
     * @param laboratoryId laboratory identifier
     * @param equipmentId Edge Gateway equipment identifier
     * @return the newly generated API key
     */
    @PostMapping("/{equipmentId}/api-key")
    @Operation(
            summary = "Generate Edge Gateway API key",
            description = "Generates an API key for an operational "
                    + "Edge Gateway assigned to a laboratory environment. "
                    + "The API key is returned only once."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "API key generated successfully",
                    content = @Content(
                            schema = @Schema(
                                    implementation =
                                            IssuedEdgeGatewayApiKeyResource.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication required"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Insufficient laboratory permissions"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Edge Gateway not found",
                    content = @Content(
                            schema = @Schema(
                                    implementation = ErrorResource.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Gateway is not eligible or "
                            + "already has an API key",
                    content = @Content(
                            schema = @Schema(
                                    implementation = ErrorResource.class
                            )
                    )
            )
    })
    public ResponseEntity<IssuedEdgeGatewayApiKeyResource> provision(
            @PathVariable Long laboratoryId,
            @PathVariable Long equipmentId) {

        var issued = apiKeyService.provision(
                laboratoryId,
                equipmentId
        );

        var resource = new IssuedEdgeGatewayApiKeyResource(
                issued.sensorExternalId(),
                issued.apiKey()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .cacheControl(CacheControl.noStore())
                .body(resource);
    }
}