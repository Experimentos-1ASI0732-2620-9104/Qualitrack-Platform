package com.iotech.qualitrack.platform.equipment.interfaces.rest;

import com.iotech.qualitrack.platform.equipment.application.queryservices.EquipmentQueryService;
import com.iotech.qualitrack.platform.equipment.domain.model.queries.GetEquipmentByEnvironmentIdQuery;
import com.iotech.qualitrack.platform.equipment.interfaces.rest.resources.EdgeGatewayCoverageResource;
import com.iotech.qualitrack.platform.equipment.interfaces.rest.transform.EdgeGatewayCoverageResourceFromEntityAssembler;
import com.iotech.qualitrack.platform.shared.application.result.ApplicationError;
import com.iotech.qualitrack.platform.shared.interfaces.rest.transform.ErrorResponseAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * Coverage of the IoT devices of an environment by its Edge Gateways.
 *
 * <p>A Gateway serves 15 devices at most, and the platform distributes the devices of the environment among its
 * Gateways. This resource tells web and mobile how that distribution looks, so they can warn the user about the
 * devices that are left without a Gateway.</p>
 */
@RestController
@RequestMapping(value = "/api/v1/laboratories/{laboratoryId}/environments/{environmentId}/edge-gateway-coverage",
        produces = APPLICATION_JSON_VALUE)
@PreAuthorize("@tenantAccess.allows('laboratoryId', #laboratoryId)")
public class EnvironmentEdgeGatewayCoverageController {

    private final EquipmentQueryService equipmentQueryService;

    /**
     * Creates the controller with the equipment query service.
     *
     * @param equipmentQueryService lists the equipment of the environment
     */
    public EnvironmentEdgeGatewayCoverageController(EquipmentQueryService equipmentQueryService) {
        this.equipmentQueryService = equipmentQueryService;
    }

    /**
     * Tells how many IoT devices each Edge Gateway of the environment serves and how many are left without one.
     *
     * @param laboratoryId laboratory that owns the environment
     * @param environmentId environment whose coverage is read
     * @return 200 with the coverage; an environment of another laboratory, or one that does not exist, is
     *         refused with 403 by the tenant access check before it reaches this method
     */
    @GetMapping
    @Operation(summary = "Get the Edge Gateway coverage of the environment",
            description = "Edge Gateways located in the environment with the number of IoT devices each one serves "
                    + "(15 at most, taken by identifier), and the number of IoT devices left without a Gateway. "
                    + "An environment without Gateways leaves all its devices uncovered.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Coverage of the environment",
                    content = @Content(schema = @Schema(implementation = EdgeGatewayCoverageResource.class))),
            @ApiResponse(responseCode = "403", description = "Laboratory or environment not available to the "
                    + "account, including an environment of another laboratory or one that does not exist")
    })
    public ResponseEntity<?> getEdgeGatewayCoverage(@PathVariable Long laboratoryId, @PathVariable Long environmentId) {
        return equipmentQueryService.handle(new GetEquipmentByEnvironmentIdQuery(laboratoryId, environmentId, false))
                .<ResponseEntity<?>>map(equipment -> ResponseEntity.ok(
                        EdgeGatewayCoverageResourceFromEntityAssembler.toResourceFromEntities(equipment)))
                .orElseGet(() -> ErrorResponseAssembler.toErrorResponseFromApplicationError(
                        ApplicationError.notFound("Environment", environmentId)));
    }
}
