package com.iotech.qualitrack.platform.equipment.interfaces.rest;

import com.iotech.qualitrack.platform.equipment.application.commandservices.BpmConfigCommandService;
import com.iotech.qualitrack.platform.equipment.application.queryservices.BpmConfigQueryService;
import com.iotech.qualitrack.platform.equipment.domain.model.entities.BpmParameterConfig;
import com.iotech.qualitrack.platform.equipment.domain.model.queries.GetBpmParameterConfigQuery;
import com.iotech.qualitrack.platform.equipment.domain.model.queries.GetBpmParameterConfigsByEquipmentIdQuery;
import com.iotech.qualitrack.platform.equipment.interfaces.rest.resources.BpmParameterConfigResource;
import com.iotech.qualitrack.platform.equipment.interfaces.rest.resources.ConfigureBpmResource;
import com.iotech.qualitrack.platform.equipment.interfaces.rest.transform.BpmConfigResourceFromEntityAssembler;
import com.iotech.qualitrack.platform.equipment.interfaces.rest.transform.ConfigureBpmCommandFromResourceAssembler;
import com.iotech.qualitrack.platform.shared.application.result.ApplicationError;
import com.iotech.qualitrack.platform.shared.application.result.Result;
import com.iotech.qualitrack.platform.shared.interfaces.rest.resources.ErrorResource;
import com.iotech.qualitrack.platform.shared.interfaces.rest.transform.ErrorResponseAssembler;
import com.iotech.qualitrack.platform.shared.interfaces.rest.transform.ResponseEntityAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * BPM parameter ranges of an equipment of the laboratory. Each parameter is a resource identified by its name, so
 * configuring it again replaces its range.
 */
@RestController
@RequestMapping(value = "/api/v1/laboratories/{laboratoryId}/equipments/{equipmentId}/bpm-configs",
        produces = APPLICATION_JSON_VALUE)
public class EquipmentBpmConfigController {

    private final BpmConfigCommandService bpmConfigCommandService;
    private final BpmConfigQueryService bpmConfigQueryService;

    public EquipmentBpmConfigController(BpmConfigCommandService bpmConfigCommandService,
                                        BpmConfigQueryService bpmConfigQueryService) {
        this.bpmConfigCommandService = bpmConfigCommandService;
        this.bpmConfigQueryService = bpmConfigQueryService;
    }

    @GetMapping
    @Operation(summary = "Get the BPM parameter ranges of an equipment")
    public ResponseEntity<List<BpmParameterConfigResource>> getBpmConfigs(@PathVariable Long laboratoryId,
                                                                          @PathVariable Long equipmentId) {
        var resources = bpmConfigQueryService.handle(new GetBpmParameterConfigsByEquipmentIdQuery(equipmentId)).stream()
                .map(BpmConfigResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
        return ResponseEntity.ok(resources);
    }

    @GetMapping("/{parameterName}")
    @Operation(summary = "Get the range of a BPM parameter of an equipment")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Range of the parameter",
                    content = @Content(schema = @Schema(implementation = BpmParameterConfigResource.class))),
            @ApiResponse(responseCode = "404", description = "The parameter is not configured",
                    content = @Content(schema = @Schema(implementation = ErrorResource.class)))
    })
    public ResponseEntity<?> getBpmConfig(@PathVariable Long laboratoryId, @PathVariable Long equipmentId,
                                          @PathVariable String parameterName) {
        return bpmConfigQueryService.handle(new GetBpmParameterConfigQuery(equipmentId, parameterName))
                .<ResponseEntity<?>>map(config -> ResponseEntity.ok(BpmConfigResourceFromEntityAssembler.toResourceFromEntity(config)))
                .orElseGet(() -> ErrorResponseAssembler.toErrorResponseFromApplicationError(
                        ApplicationError.notFound("BpmParameterConfig", parameterName)));
    }

    @PutMapping(value = "/{parameterName}", consumes = APPLICATION_JSON_VALUE)
    @Operation(summary = "Configure the range of a BPM parameter of an equipment",
            description = "Creates the range of the parameter or replaces the one configured.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Range saved",
                    content = @Content(schema = @Schema(implementation = BpmParameterConfigResource.class))),
            @ApiResponse(responseCode = "400", description = "Missing value or minimum not lower than maximum",
                    content = @Content(schema = @Schema(implementation = ErrorResource.class))),
            @ApiResponse(responseCode = "403", description = "Equipment not available to the account")
    })
    public ResponseEntity<?> configureBpmParameter(@PathVariable Long laboratoryId, @PathVariable Long equipmentId,
                                                   @PathVariable String parameterName,
                                                   @RequestBody ConfigureBpmResource resource) {
        var command = ConfigureBpmCommandFromResourceAssembler.toCommandFromResource(equipmentId, parameterName, resource);
        var result = bpmConfigCommandService.handle(command)
                .flatMap(configId -> bpmConfigQueryService.handle(new GetBpmParameterConfigQuery(equipmentId, command.parameterName()))
                        .<Result<BpmParameterConfig, ApplicationError>>map(Result::success)
                        .orElseGet(() -> Result.failure(ApplicationError.notFound("BpmParameterConfig", configId))));
        return ResponseEntityAssembler.toResponseEntityFromResult(result,
                BpmConfigResourceFromEntityAssembler::toResourceFromEntity, HttpStatus.OK);
    }
}
