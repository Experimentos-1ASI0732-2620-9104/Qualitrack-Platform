package com.iotech.qualitrack.platform.tracking.interfaces.rest;

import com.iotech.qualitrack.platform.shared.application.result.ApplicationError;
import com.iotech.qualitrack.platform.shared.interfaces.rest.resources.ErrorResource;
import com.iotech.qualitrack.platform.shared.interfaces.rest.transform.ErrorResponseAssembler;
import com.iotech.qualitrack.platform.shared.interfaces.rest.transform.ResponseEntityAssembler;
import com.iotech.qualitrack.platform.tracking.application.commandservices.TrackingCommandService;
import com.iotech.qualitrack.platform.tracking.application.queryservices.TrackingQueryService;
import com.iotech.qualitrack.platform.tracking.domain.model.queries.GetEnvironmentProfileQuery;
import com.iotech.qualitrack.platform.tracking.domain.model.queries.GetMeasurementsQuery;
import com.iotech.qualitrack.platform.tracking.interfaces.rest.resources.EnvironmentalProfileResource;
import com.iotech.qualitrack.platform.tracking.interfaces.rest.resources.MeasurementResource;
import com.iotech.qualitrack.platform.tracking.interfaces.rest.resources.RecordMeasurementResource;
import com.iotech.qualitrack.platform.tracking.interfaces.rest.resources.UpdateThresholdsResource;
import com.iotech.qualitrack.platform.tracking.interfaces.rest.transform.EnvironmentalProfileCommandFromResourceAssembler;
import com.iotech.qualitrack.platform.tracking.interfaces.rest.transform.EnvironmentalProfileResourceFromEntityAssembler;
import com.iotech.qualitrack.platform.tracking.interfaces.rest.transform.MeasurementResourceFromEntityAssembler;
import com.iotech.qualitrack.platform.tracking.interfaces.rest.transform.RecordMeasurementCommandFromResourceAssembler;
import com.iotech.qualitrack.platform.tracking.interfaces.rest.transform.TelemetryResponseAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * Environmental profile and telemetry of an environment, measured by its environmental device
 * (US56, US60, US61, US68, TS42, TS54, TS58).
 */
@RestController
@RequestMapping(value = "/api/v1/laboratories/{laboratoryId}/environments/{environmentId}", produces = APPLICATION_JSON_VALUE)
@PreAuthorize("@tenantAccess.allows('laboratoryId', #laboratoryId)")
public class EnvironmentTelemetryController {
    private static final String QUALITY_ROLES =
            "@tenantAccess.allows('laboratoryId', #laboratoryId) and hasAnyAuthority('ROLE_QA_MANAGER', 'ROLE_ADMIN')";

    private final TrackingCommandService trackingCommandService;
    private final TrackingQueryService trackingQueryService;

    public EnvironmentTelemetryController(TrackingCommandService trackingCommandService,
                                          TrackingQueryService trackingQueryService) {
        this.trackingCommandService = trackingCommandService;
        this.trackingQueryService = trackingQueryService;
    }

    @GetMapping("/environmental-profile")
    @Operation(summary = "Get the environmental profile of the environment",
            description = "Air quality thresholds in force and their version.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile of the environment",
                    content = @Content(schema = @Schema(implementation = EnvironmentalProfileResource.class))),
            @ApiResponse(responseCode = "403", description = "Environment not available to the account"),
            @ApiResponse(responseCode = "404", description = "The environment has no profile yet",
                    content = @Content(schema = @Schema(implementation = ErrorResource.class)))
    })
    public ResponseEntity<?> getProfile(@PathVariable Long laboratoryId, @PathVariable Long environmentId) {
        return trackingQueryService.handle(new GetEnvironmentProfileQuery(laboratoryId, environmentId))
                .<ResponseEntity<?>>map(profile -> ResponseEntity.ok(
                        EnvironmentalProfileResourceFromEntityAssembler.toResourceFromEntity(profile)))
                .orElseGet(() -> ErrorResponseAssembler.toErrorResponseFromApplicationError(
                        ApplicationError.notFound("EnvironmentalProfile", environmentId)));
    }

    @PutMapping(value = "/environmental-profile/thresholds", consumes = APPLICATION_JSON_VALUE)
    @PreAuthorize(QUALITY_ROLES)
    @Operation(summary = "Save the thresholds of the environment",
            description = "Replaces the air quality thresholds evaluated with the readings of the environmental device. "
                    + "The environment needs an environmental device; each change increases the profile version.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Thresholds saved",
                    content = @Content(schema = @Schema(implementation = EnvironmentalProfileResource.class))),
            @ApiResponse(responseCode = "400", description = "Contradictory or incomplete limits, or environment without environmental device",
                    content = @Content(schema = @Schema(implementation = ErrorResource.class))),
            @ApiResponse(responseCode = "403", description = "Environment not available to the account or missing quality role")
    })
    public ResponseEntity<?> updateThresholds(@PathVariable Long laboratoryId, @PathVariable Long environmentId,
                                              @Valid @RequestBody UpdateThresholdsResource resource) {
        var command = EnvironmentalProfileCommandFromResourceAssembler.toEnvironmentCommand(laboratoryId, environmentId, resource);
        return ResponseEntityAssembler.toResponseEntityFromResult(trackingCommandService.handle(command),
                EnvironmentalProfileResourceFromEntityAssembler::toResourceFromEntity, HttpStatus.OK);
    }

    @PostMapping(value = "/telemetry-measurements", consumes = APPLICATION_JSON_VALUE)
    @Operation(summary = "Record a reading of the environmental device",
            description = "Synchronizes a reading from the Edge. The platform evaluates it with the profile of the environment "
                    + "and reports a deviation to Compliance & Alerting when the condition gets worse. A reading sent again "
                    + "(same metric and moment) is returned with 200 and not stored twice.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Reading recorded",
                    content = @Content(schema = @Schema(implementation = MeasurementResource.class))),
            @ApiResponse(responseCode = "200", description = "Reading already received",
                    content = @Content(schema = @Schema(implementation = MeasurementResource.class))),
            @ApiResponse(responseCode = "400", description = "Invalid reading or environment without environmental device",
                    content = @Content(schema = @Schema(implementation = ErrorResource.class))),
            @ApiResponse(responseCode = "403", description = "Environment not available to the account")
    })
    public ResponseEntity<?> recordMeasurement(@PathVariable Long laboratoryId, @PathVariable Long environmentId,
                                               @Valid @RequestBody RecordMeasurementResource resource) {
        var command = RecordMeasurementCommandFromResourceAssembler.toCommandFromResource(laboratoryId, environmentId,
                null, resource);
        return TelemetryResponseAssembler.toRecordedResponse(trackingCommandService.handle(command),
                MeasurementResourceFromEntityAssembler::toResourceFromEntity);
    }

    @GetMapping("/telemetry-measurements")
    @Operation(summary = "Get the readings of the environmental device",
            description = "Air quality and motion readings of the period, oldest first. By default the last 24 hours.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Readings of the period",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = MeasurementResource.class)))),
            @ApiResponse(responseCode = "400", description = "Invalid period or metric",
                    content = @Content(schema = @Schema(implementation = ErrorResource.class))),
            @ApiResponse(responseCode = "403", description = "Environment not available to the account"),
            @ApiResponse(responseCode = "404", description = "The environment has no environmental device",
                    content = @Content(schema = @Schema(implementation = ErrorResource.class)))
    })
    public ResponseEntity<?> getMeasurements(@PathVariable Long laboratoryId, @PathVariable Long environmentId,
                                             @Parameter(description = "Start of the period (ISO-8601 with offset)") @RequestParam(required = false) String from,
                                             @Parameter(description = "End of the period (ISO-8601 with offset)") @RequestParam(required = false) String to,
                                             @Parameter(description = "AIR_QUALITY or MOTION") @RequestParam(required = false) String metric) {
        var period = TelemetryResponseAssembler.period(from, to);
        return trackingQueryService.handle(new GetMeasurementsQuery(laboratoryId, environmentId, null,
                        TelemetryResponseAssembler.metric(metric), period[0], period[1]))
                .<ResponseEntity<?>>map(readings -> ResponseEntity.ok(readings.stream()
                        .map(MeasurementResourceFromEntityAssembler::toResourceFromEntity).toList()))
                .orElseGet(() -> ErrorResponseAssembler.toErrorResponseFromApplicationError(
                        ApplicationError.notFound("EnvironmentalDevice", environmentId)));
    }
}
