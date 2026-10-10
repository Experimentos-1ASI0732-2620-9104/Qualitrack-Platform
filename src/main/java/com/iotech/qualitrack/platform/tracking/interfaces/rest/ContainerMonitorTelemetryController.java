package com.iotech.qualitrack.platform.tracking.interfaces.rest;

import com.iotech.qualitrack.platform.shared.application.result.ApplicationError;
import com.iotech.qualitrack.platform.shared.interfaces.rest.resources.ErrorResource;
import com.iotech.qualitrack.platform.shared.interfaces.rest.transform.ErrorResponseAssembler;
import com.iotech.qualitrack.platform.shared.interfaces.rest.transform.ResponseEntityAssembler;
import com.iotech.qualitrack.platform.tracking.application.commandservices.TrackingCommandService;
import com.iotech.qualitrack.platform.tracking.application.queryservices.TrackingQueryService;
import com.iotech.qualitrack.platform.tracking.domain.model.queries.GetActuationEventsQuery;
import com.iotech.qualitrack.platform.tracking.domain.model.queries.GetContainerMonitorProfileQuery;
import com.iotech.qualitrack.platform.tracking.domain.model.queries.GetMeasurementsQuery;
import com.iotech.qualitrack.platform.tracking.interfaces.rest.resources.ActuationEventResource;
import com.iotech.qualitrack.platform.tracking.interfaces.rest.resources.EnvironmentalProfileResource;
import com.iotech.qualitrack.platform.tracking.interfaces.rest.resources.MeasurementResource;
import com.iotech.qualitrack.platform.tracking.interfaces.rest.resources.RecordActuationEventResource;
import com.iotech.qualitrack.platform.tracking.interfaces.rest.resources.RecordMeasurementResource;
import com.iotech.qualitrack.platform.tracking.interfaces.rest.resources.UpdateActuationRulesResource;
import com.iotech.qualitrack.platform.tracking.interfaces.rest.resources.UpdateThresholdsResource;
import com.iotech.qualitrack.platform.tracking.interfaces.rest.transform.ActuationEventResourceFromEntityAssembler;
import com.iotech.qualitrack.platform.tracking.interfaces.rest.transform.EnvironmentalProfileCommandFromResourceAssembler;
import com.iotech.qualitrack.platform.tracking.interfaces.rest.transform.EnvironmentalProfileResourceFromEntityAssembler;
import com.iotech.qualitrack.platform.tracking.interfaces.rest.transform.MeasurementResourceFromEntityAssembler;
import com.iotech.qualitrack.platform.tracking.interfaces.rest.transform.RecordActuationEventCommandFromResourceAssembler;
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
 * Environmental profile, telemetry and actions of a container monitor
 * (US57, US58, US59, US63, US64, US65, US66, US69, TS43, TS44, TS45, TS55, TS56, TS59, TS60).
 */
@RestController
@RequestMapping(value = "/api/v1/laboratories/{laboratoryId}/environments/{environmentId}/container-monitors/{deviceId}",
        produces = APPLICATION_JSON_VALUE)
@PreAuthorize("@tenantAccess.allows('laboratoryId', #laboratoryId)")
public class ContainerMonitorTelemetryController {
    private static final String QUALITY_ROLES =
            "@tenantAccess.allows('laboratoryId', #laboratoryId) and hasAnyAuthority('ROLE_QA_MANAGER', 'ROLE_ADMIN')";

    private final TrackingCommandService trackingCommandService;
    private final TrackingQueryService trackingQueryService;

    public ContainerMonitorTelemetryController(TrackingCommandService trackingCommandService,
                                               TrackingQueryService trackingQueryService) {
        this.trackingCommandService = trackingCommandService;
        this.trackingQueryService = trackingQueryService;
    }

    @GetMapping("/environmental-profile")
    @Operation(summary = "Get the environmental profile of the container monitor",
            description = "Temperature, humidity and luminosity thresholds, actuation rules and their version.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile of the container monitor",
                    content = @Content(schema = @Schema(implementation = EnvironmentalProfileResource.class))),
            @ApiResponse(responseCode = "403", description = "Device not available to the account"),
            @ApiResponse(responseCode = "404", description = "Not a container monitor of the environment, or no profile yet",
                    content = @Content(schema = @Schema(implementation = ErrorResource.class)))
    })
    public ResponseEntity<?> getProfile(@PathVariable Long laboratoryId, @PathVariable Long environmentId,
                                        @PathVariable Long deviceId) {
        return trackingQueryService.handle(new GetContainerMonitorProfileQuery(laboratoryId, environmentId, deviceId))
                .<ResponseEntity<?>>map(profile -> ResponseEntity.ok(
                        EnvironmentalProfileResourceFromEntityAssembler.toResourceFromEntity(profile)))
                .orElseGet(() -> ErrorResponseAssembler.toErrorResponseFromApplicationError(
                        ApplicationError.notFound("EnvironmentalProfile", deviceId)));
    }

    @PutMapping(value = "/environmental-profile/thresholds", consumes = APPLICATION_JSON_VALUE)
    @PreAuthorize(QUALITY_ROLES)
    @Operation(summary = "Save the thresholds of the container monitor",
            description = "Replaces the temperature, humidity and luminosity thresholds. A threshold used by an actuation rule "
                    + "cannot be removed. Each change increases the profile version.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Thresholds saved",
                    content = @Content(schema = @Schema(implementation = EnvironmentalProfileResource.class))),
            @ApiResponse(responseCode = "400", description = "Contradictory or incomplete limits, or not a container monitor of the environment",
                    content = @Content(schema = @Schema(implementation = ErrorResource.class))),
            @ApiResponse(responseCode = "403", description = "Device not available to the account or missing quality role")
    })
    public ResponseEntity<?> updateThresholds(@PathVariable Long laboratoryId, @PathVariable Long environmentId,
                                              @PathVariable Long deviceId,
                                              @Valid @RequestBody UpdateThresholdsResource resource) {
        var command = EnvironmentalProfileCommandFromResourceAssembler.toContainerMonitorCommand(laboratoryId,
                environmentId, deviceId, resource);
        return ResponseEntityAssembler.toResponseEntityFromResult(trackingCommandService.handle(command),
                EnvironmentalProfileResourceFromEntityAssembler::toResourceFromEntity, HttpStatus.OK);
    }

    @PutMapping(value = "/environmental-profile/actuation-rules", consumes = APPLICATION_JSON_VALUE)
    @PreAuthorize(QUALITY_ROLES)
    @Operation(summary = "Save the actuation rules of the container monitor",
            description = "Relates a WARNING or CRITICAL condition of a metric with the ventilation, the simulated "
                    + "refrigeration or the servo. The metric needs a threshold in the profile.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rules saved",
                    content = @Content(schema = @Schema(implementation = EnvironmentalProfileResource.class))),
            @ApiResponse(responseCode = "400", description = "Incomplete or repeated rule, metric without threshold, or not a container monitor",
                    content = @Content(schema = @Schema(implementation = ErrorResource.class))),
            @ApiResponse(responseCode = "403", description = "Device not available to the account or missing quality role")
    })
    public ResponseEntity<?> updateActuationRules(@PathVariable Long laboratoryId, @PathVariable Long environmentId,
                                                  @PathVariable Long deviceId,
                                                  @Valid @RequestBody UpdateActuationRulesResource resource) {
        var command = EnvironmentalProfileCommandFromResourceAssembler.toActuationRulesCommand(laboratoryId,
                environmentId, deviceId, resource);
        return ResponseEntityAssembler.toResponseEntityFromResult(trackingCommandService.handle(command),
                EnvironmentalProfileResourceFromEntityAssembler::toResourceFromEntity, HttpStatus.OK);
    }

    @PostMapping(value = "/telemetry-measurements", consumes = APPLICATION_JSON_VALUE)
    @Operation(summary = "Record a reading of the container monitor",
            description = "Synchronizes a temperature, humidity, luminosity or RFID reading from the Edge. The platform "
                    + "evaluates it with the profile of the container monitor. A reading sent again (same metric and moment) "
                    + "is returned with 200 and not stored twice.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Reading recorded",
                    content = @Content(schema = @Schema(implementation = MeasurementResource.class))),
            @ApiResponse(responseCode = "200", description = "Reading already received",
                    content = @Content(schema = @Schema(implementation = MeasurementResource.class))),
            @ApiResponse(responseCode = "400", description = "Invalid reading or not a container monitor of the environment",
                    content = @Content(schema = @Schema(implementation = ErrorResource.class))),
            @ApiResponse(responseCode = "403", description = "Device not available to the account")
    })
    public ResponseEntity<?> recordMeasurement(@PathVariable Long laboratoryId, @PathVariable Long environmentId,
                                               @PathVariable Long deviceId,
                                               @Valid @RequestBody RecordMeasurementResource resource) {
        var command = RecordMeasurementCommandFromResourceAssembler.toCommandFromResource(laboratoryId, environmentId,
                deviceId, resource);
        return TelemetryResponseAssembler.toRecordedResponse(trackingCommandService.handle(command),
                MeasurementResourceFromEntityAssembler::toResourceFromEntity);
    }

    @GetMapping("/telemetry-measurements")
    @Operation(summary = "Get the readings of the container monitor",
            description = "Readings of the period, oldest first. By default the last 24 hours.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Readings of the period",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = MeasurementResource.class)))),
            @ApiResponse(responseCode = "400", description = "Invalid period or metric",
                    content = @Content(schema = @Schema(implementation = ErrorResource.class))),
            @ApiResponse(responseCode = "403", description = "Device not available to the account"),
            @ApiResponse(responseCode = "404", description = "Not a container monitor of the environment",
                    content = @Content(schema = @Schema(implementation = ErrorResource.class)))
    })
    public ResponseEntity<?> getMeasurements(@PathVariable Long laboratoryId, @PathVariable Long environmentId,
                                             @PathVariable Long deviceId,
                                             @Parameter(description = "Start of the period (ISO-8601 with offset)") @RequestParam(required = false) String from,
                                             @Parameter(description = "End of the period (ISO-8601 with offset)") @RequestParam(required = false) String to,
                                             @Parameter(description = "TEMPERATURE, HUMIDITY, LUMINOSITY or RFID_TAG") @RequestParam(required = false) String metric) {
        var period = TelemetryResponseAssembler.period(from, to);
        return trackingQueryService.handle(new GetMeasurementsQuery(laboratoryId, environmentId, deviceId,
                        TelemetryResponseAssembler.metric(metric), period[0], period[1]))
                .<ResponseEntity<?>>map(readings -> ResponseEntity.ok(readings.stream()
                        .map(MeasurementResourceFromEntityAssembler::toResourceFromEntity).toList()))
                .orElseGet(() -> ErrorResponseAssembler.toErrorResponseFromApplicationError(
                        ApplicationError.notFound("ContainerMonitor", deviceId)));
    }

    @PostMapping(value = "/actuation-events", consumes = APPLICATION_JSON_VALUE)
    @Operation(summary = "Record an action of the container monitor",
            description = "Synchronizes from the Edge an action the device executed locally (ventilation, simulated "
                    + "refrigeration or servo) and its cause. An action sent again (same action and moment) is returned with 200.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Action recorded",
                    content = @Content(schema = @Schema(implementation = ActuationEventResource.class))),
            @ApiResponse(responseCode = "200", description = "Action already received",
                    content = @Content(schema = @Schema(implementation = ActuationEventResource.class))),
            @ApiResponse(responseCode = "400", description = "Invalid action or not a container monitor of the environment",
                    content = @Content(schema = @Schema(implementation = ErrorResource.class))),
            @ApiResponse(responseCode = "403", description = "Device not available to the account")
    })
    public ResponseEntity<?> recordActuationEvent(@PathVariable Long laboratoryId, @PathVariable Long environmentId,
                                                  @PathVariable Long deviceId,
                                                  @Valid @RequestBody RecordActuationEventResource resource) {
        var command = RecordActuationEventCommandFromResourceAssembler.toCommandFromResource(laboratoryId, environmentId,
                deviceId, resource);
        return TelemetryResponseAssembler.toRecordedResponse(trackingCommandService.handle(command),
                ActuationEventResourceFromEntityAssembler::toResourceFromEntity);
    }

    @GetMapping("/actuation-events")
    @Operation(summary = "Get the actions of the container monitor",
            description = "Actions of the period, oldest first. By default the last 24 hours.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Actions of the period",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ActuationEventResource.class)))),
            @ApiResponse(responseCode = "400", description = "Invalid period",
                    content = @Content(schema = @Schema(implementation = ErrorResource.class))),
            @ApiResponse(responseCode = "403", description = "Device not available to the account"),
            @ApiResponse(responseCode = "404", description = "Not a container monitor of the environment",
                    content = @Content(schema = @Schema(implementation = ErrorResource.class)))
    })
    public ResponseEntity<?> getActuationEvents(@PathVariable Long laboratoryId, @PathVariable Long environmentId,
                                                @PathVariable Long deviceId,
                                                @Parameter(description = "Start of the period (ISO-8601 with offset)") @RequestParam(required = false) String from,
                                                @Parameter(description = "End of the period (ISO-8601 with offset)") @RequestParam(required = false) String to) {
        var period = TelemetryResponseAssembler.period(from, to);
        return trackingQueryService.handle(new GetActuationEventsQuery(laboratoryId, environmentId, deviceId, period[0], period[1]))
                .<ResponseEntity<?>>map(events -> ResponseEntity.ok(events.stream()
                        .map(ActuationEventResourceFromEntityAssembler::toResourceFromEntity).toList()))
                .orElseGet(() -> ErrorResponseAssembler.toErrorResponseFromApplicationError(
                        ApplicationError.notFound("ContainerMonitor", deviceId)));
    }
}
