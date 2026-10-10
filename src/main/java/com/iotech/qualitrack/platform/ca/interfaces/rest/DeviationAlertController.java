package com.iotech.qualitrack.platform.ca.interfaces.rest;

import com.iotech.qualitrack.platform.ca.application.commandservices.CaCommandService;
import com.iotech.qualitrack.platform.ca.application.commandservices.NotificationCommandService;
import com.iotech.qualitrack.platform.ca.application.queryservices.CaQueryService;
import com.iotech.qualitrack.platform.ca.domain.model.aggregates.DeviationAlert;
import com.iotech.qualitrack.platform.ca.domain.model.commands.AcknowledgeAlertCommand;
import com.iotech.qualitrack.platform.ca.domain.model.commands.SendAlertEmailNotificationCommand;
import com.iotech.qualitrack.platform.ca.domain.model.queries.GetAlertByIdQuery;
import com.iotech.qualitrack.platform.ca.domain.model.queries.GetAlertDetailQuery;
import com.iotech.qualitrack.platform.ca.domain.model.queries.GetAlertsQuery;
import com.iotech.qualitrack.platform.ca.domain.model.valueobjects.AlertSeverity;
import com.iotech.qualitrack.platform.ca.domain.model.valueobjects.AlertStatus;
import com.iotech.qualitrack.platform.ca.domain.model.valueobjects.DeviationRegistration;
import com.iotech.qualitrack.platform.ca.interfaces.rest.resources.AlertEmailNotificationResource;
import com.iotech.qualitrack.platform.ca.interfaces.rest.resources.CreateDeviationAlertResource;
import com.iotech.qualitrack.platform.ca.interfaces.rest.resources.DeviationAlertDetailResource;
import com.iotech.qualitrack.platform.ca.interfaces.rest.resources.DeviationAlertResource;
import com.iotech.qualitrack.platform.ca.interfaces.rest.resources.ResolveAlertResource;
import com.iotech.qualitrack.platform.ca.interfaces.rest.transform.CreateDeviationAlertCommandFromResourceAssembler;
import com.iotech.qualitrack.platform.ca.interfaces.rest.transform.DeviationAlertResourceFromEntityAssembler;
import com.iotech.qualitrack.platform.ca.interfaces.rest.transform.ResolveAlertCommandFromResourceAssembler;
import com.iotech.qualitrack.platform.shared.application.result.ApplicationError;
import com.iotech.qualitrack.platform.shared.application.result.Result;
import com.iotech.qualitrack.platform.shared.application.security.CurrentUser;
import com.iotech.qualitrack.platform.shared.interfaces.rest.resources.ErrorResource;
import com.iotech.qualitrack.platform.shared.interfaces.rest.transform.ErrorResponseAssembler;
import com.iotech.qualitrack.platform.shared.interfaces.rest.transform.ResponseEntityAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * Deviation alerts of the environments of a laboratory and their lifecycle (TS73-TS76): alerts are registered and
 * listed per environment, and acknowledged and resolved by registering an acknowledgement or a resolution, with the
 * authenticated user as actor. While an alert is open, new deviations of the same device and variable are added to it.
 */
@RestController
@RequestMapping(value = "/api/v1", produces = APPLICATION_JSON_VALUE)
public class DeviationAlertController {

    private final CaCommandService caCommandService;
    private final CaQueryService caQueryService;
    private final NotificationCommandService notificationCommandService;
    private final CurrentUser currentUser;

    public DeviationAlertController(CaCommandService caCommandService, CaQueryService caQueryService,
                                    NotificationCommandService notificationCommandService, CurrentUser currentUser) {
        this.caCommandService = caCommandService;
        this.caQueryService = caQueryService;
        this.notificationCommandService = notificationCommandService;
        this.currentUser = currentUser;
    }

    @GetMapping("/deviation-alerts/{alertId}")
    @Operation(summary = "Get a deviation alert",
            description = "Origin (environment or container), variable, value, severity, lifecycle and the actions the "
                    + "container monitor executed for the variable while the incident was open (US86).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Deviation alert",
                    content = @Content(schema = @Schema(implementation = DeviationAlertDetailResource.class))),
            @ApiResponse(responseCode = "403", description = "Alert not available to the account"),
            @ApiResponse(responseCode = "404", description = "Alert not found",
                    content = @Content(schema = @Schema(implementation = ErrorResource.class)))
    })
    public ResponseEntity<?> getAlertById(@PathVariable Long alertId) {
        return caQueryService.handle(new GetAlertDetailQuery(alertId))
                .<ResponseEntity<?>>map(detail -> ResponseEntity.ok(DeviationAlertResourceFromEntityAssembler.toDetailResourceFromEntity(detail)))
                .orElseGet(() -> ErrorResponseAssembler.toErrorResponseFromApplicationError(
                        ApplicationError.notFound("DeviationAlert", alertId)));
    }

    @GetMapping("/laboratories/{laboratoryId}/environments/{environmentId}/deviation-alerts")
    @Operation(summary = "Get the deviation alerts of an environment",
            description = "Alerts of the environment and its monitored containers, newest first (US85, TS74). Optional "
                    + "filters: status (UNRESOLVED, ACKNOWLEDGED, RESOLVED), severity (LOW, WARNING, CRITICAL), deviceId and "
                    + "active=true to keep only open alerts (unresolved or being attended).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Alerts of the environment",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = DeviationAlertResource.class)))),
            @ApiResponse(responseCode = "400", description = "Unknown status or severity",
                    content = @Content(schema = @Schema(implementation = ErrorResource.class))),
            @ApiResponse(responseCode = "403", description = "Environment not available to the account"),
            @ApiResponse(responseCode = "404", description = "Environment not found",
                    content = @Content(schema = @Schema(implementation = ErrorResource.class)))
    })
    public ResponseEntity<?> getEnvironmentAlerts(@PathVariable Long laboratoryId, @PathVariable Long environmentId,
                                                  @RequestParam(required = false) String status,
                                                  @RequestParam(required = false) String severity,
                                                  @RequestParam(required = false) Long deviceId,
                                                  @RequestParam(defaultValue = "false") boolean active) {
        AlertStatus statusFilter;
        AlertSeverity severityFilter;
        try {
            statusFilter = status == null || status.isBlank() ? null : AlertStatus.valueOf(status.trim().toUpperCase());
            severityFilter = severity == null || severity.isBlank() ? null : AlertSeverity.valueOf(severity.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            return ErrorResponseAssembler.toErrorResponseFromApplicationError(
                    ApplicationError.validationError("filters", "Unknown alert status or severity"));
        }
        List<DeviationAlertResource> resources = caQueryService
                .handle(new GetAlertsQuery(laboratoryId, environmentId, deviceId, statusFilter, severityFilter, active)).stream()
                .map(DeviationAlertResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
        return ResponseEntity.ok(resources);
    }

    @PostMapping(value = "/laboratories/{laboratoryId}/environments/{environmentId}/deviation-alerts",
            consumes = APPLICATION_JSON_VALUE)
    @Operation(summary = "Register a deviation of an environment or container",
            description = "Opens the alert of a deviation confirmed by Cloud for the environmental device or a container "
                    + "monitor of the environment (TS73). When the same device and variable already have an open alert, "
                    + "the deviation is added to that incident (its severity only rises) and the alert is returned with 200.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Alert opened",
                    content = @Content(schema = @Schema(implementation = DeviationAlertResource.class))),
            @ApiResponse(responseCode = "200", description = "Deviation added to the open alert of the incident",
                    content = @Content(schema = @Schema(implementation = DeviationAlertResource.class))),
            @ApiResponse(responseCode = "400", description = "Missing data, unknown severity or device not located in the environment",
                    content = @Content(schema = @Schema(implementation = ErrorResource.class))),
            @ApiResponse(responseCode = "403", description = "Environment not available to the account or read-only user"),
            @ApiResponse(responseCode = "404", description = "Environment not found",
                    content = @Content(schema = @Schema(implementation = ErrorResource.class)))
    })
    public ResponseEntity<?> registerDeviation(@PathVariable Long laboratoryId, @PathVariable Long environmentId,
                                               @RequestBody CreateDeviationAlertResource resource) {
        var command = CreateDeviationAlertCommandFromResourceAssembler.toCommandFromResource(laboratoryId, environmentId, resource);
        var result = caCommandService.handle(command);
        if (result instanceof Result.Success<DeviationRegistration, ApplicationError> success && !success.value().created()) {
            return ResponseEntity.ok(DeviationAlertResourceFromEntityAssembler.toResourceFromEntity(success.value().alert()));
        }
        return ResponseEntityAssembler.toCreatedResponseEntityAtLocation(result,
                registration -> DeviationAlertResourceFromEntityAssembler.toResourceFromEntity(registration.alert()),
                registration -> ServletUriComponentsBuilder.fromCurrentContextPath()
                        .path("/api/v1/deviation-alerts/{alertId}").buildAndExpand(registration.alert().getId()).toUri());
    }

    @PostMapping("/deviation-alerts/{alertId}/acknowledgements")
    @Operation(summary = "Acknowledge a deviation alert",
            description = "Registers that the authenticated user is attending the alert (US87, TS75).")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Alert acknowledged",
                    content = @Content(schema = @Schema(implementation = DeviationAlertResource.class))),
            @ApiResponse(responseCode = "403", description = "Alert not available to the account or read-only user"),
            @ApiResponse(responseCode = "404", description = "Alert not found",
                    content = @Content(schema = @Schema(implementation = ErrorResource.class))),
            @ApiResponse(responseCode = "409", description = "The alert is already acknowledged or resolved",
                    content = @Content(schema = @Schema(implementation = ErrorResource.class)))
    })
    public ResponseEntity<?> acknowledgeAlert(@PathVariable Long alertId) {
        return toAlertResponse(caCommandService.handle(new AcknowledgeAlertCommand(alertId, currentUser.userId())));
    }

    @PostMapping(value = "/deviation-alerts/{alertId}/resolutions", consumes = APPLICATION_JSON_VALUE)
    @Operation(summary = "Resolve a deviation alert",
            description = "Registers the resolution of the alert by the authenticated user with its notes (US88, TS76). "
                    + "The incident is closed; a later deviation opens a new alert.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Alert resolved",
                    content = @Content(schema = @Schema(implementation = DeviationAlertResource.class))),
            @ApiResponse(responseCode = "400", description = "Resolution notes missing",
                    content = @Content(schema = @Schema(implementation = ErrorResource.class))),
            @ApiResponse(responseCode = "403", description = "Alert not available to the account or read-only user"),
            @ApiResponse(responseCode = "404", description = "Alert not found",
                    content = @Content(schema = @Schema(implementation = ErrorResource.class))),
            @ApiResponse(responseCode = "409", description = "The alert is already resolved",
                    content = @Content(schema = @Schema(implementation = ErrorResource.class)))
    })
    public ResponseEntity<?> resolveAlert(@PathVariable Long alertId, @RequestBody ResolveAlertResource resource) {
        var command = ResolveAlertCommandFromResourceAssembler.toCommandFromResource(alertId, currentUser.userId(), resource);
        return toAlertResponse(caCommandService.handle(command));
    }

    @PostMapping("/deviation-alerts/{alertId}/email-notifications")
    @Operation(summary = "E-mail a critical alert to the laboratory",
            description = "Sends again the e-mail notice of an open critical alert to the people of the laboratory who "
                    + "enabled e-mail notices, except the requester (US84, TS78). QualiTrack already sends it on its own "
                    + "when an alert opens as critical or escalates to critical.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "E-mail notice sent",
                    content = @Content(schema = @Schema(implementation = AlertEmailNotificationResource.class))),
            @ApiResponse(responseCode = "403", description = "Alert not available to the account or read-only user"),
            @ApiResponse(responseCode = "404", description = "Alert not found",
                    content = @Content(schema = @Schema(implementation = ErrorResource.class))),
            @ApiResponse(responseCode = "409", description = "The alert is not critical or is already resolved",
                    content = @Content(schema = @Schema(implementation = ErrorResource.class))),
            @ApiResponse(responseCode = "502", description = "The e-mail provider did not accept the notices",
                    content = @Content(schema = @Schema(implementation = ErrorResource.class)))
    })
    public ResponseEntity<?> sendEmailNotification(@PathVariable Long alertId) {
        var result = notificationCommandService.handle(new SendAlertEmailNotificationCommand(alertId, currentUser.userId()));
        return ResponseEntityAssembler.toResponseEntityFromResult(result,
                delivery -> new AlertEmailNotificationResource(delivery.alertId(), delivery.recipients(),
                        delivery.delivered(), delivery.sentAt()), HttpStatus.CREATED);
    }

    private ResponseEntity<?> toAlertResponse(Result<Long, ApplicationError> commandResult) {
        var result = commandResult.flatMap(alertId -> caQueryService.handle(new GetAlertByIdQuery(alertId))
                .<Result<DeviationAlert, ApplicationError>>map(Result::success)
                .orElseGet(() -> Result.failure(ApplicationError.notFound("DeviationAlert", alertId))));
        return ResponseEntityAssembler.toResponseEntityFromResult(result,
                DeviationAlertResourceFromEntityAssembler::toResourceFromEntity, HttpStatus.CREATED);
    }
}
