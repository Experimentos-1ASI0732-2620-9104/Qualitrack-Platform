package com.iotech.qualitrack.platform.ca.interfaces.rest;

import com.iotech.qualitrack.platform.ca.application.commandservices.CaCommandService;
import com.iotech.qualitrack.platform.ca.application.queryservices.CaQueryService;
import com.iotech.qualitrack.platform.ca.domain.model.entities.NotificationPreference;
import com.iotech.qualitrack.platform.ca.domain.model.queries.GetNotificationPreferenceByUserIdQuery;
import com.iotech.qualitrack.platform.ca.interfaces.rest.resources.NotificationPreferenceResource;
import com.iotech.qualitrack.platform.ca.interfaces.rest.resources.UpdateNotificationPreferenceResource;
import com.iotech.qualitrack.platform.ca.interfaces.rest.transform.NotificationPreferenceResourceFromEntityAssembler;
import com.iotech.qualitrack.platform.ca.interfaces.rest.transform.UpdateNotificationPreferenceCommandFromResourceAssembler;
import com.iotech.qualitrack.platform.shared.application.security.CurrentUser;
import com.iotech.qualitrack.platform.shared.interfaces.rest.resources.ErrorResource;
import com.iotech.qualitrack.platform.shared.interfaces.rest.transform.ResponseEntityAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * Notification preferences of the authenticated user: whether notifications reach the bell of the web application
 * and the e-mail, and the lowest alert severity that is notified.
 */
@RestController
@RequestMapping(
        value = "/api/v1/users/me/notification-preferences",
        produces = APPLICATION_JSON_VALUE
)
public class NotificationPreferenceController {

    private final CaCommandService caCommandService;
    private final CaQueryService caQueryService;
    private final CurrentUser currentUser;

    public NotificationPreferenceController(
            CaCommandService caCommandService,
            CaQueryService caQueryService,
            CurrentUser currentUser
    ) {
        this.caCommandService = caCommandService;
        this.caQueryService = caQueryService;
        this.currentUser = currentUser;
    }

    @GetMapping
    @Operation(summary = "Get the notification preferences of the authenticated user",
            description = "Users who never saved them get the defaults: bell and e-mail enabled, alerts from WARNING.")
    @ApiResponse(responseCode = "200", description = "Notification preferences",
            content = @Content(schema = @Schema(implementation = NotificationPreferenceResource.class)))
    public ResponseEntity<NotificationPreferenceResource> getPreferences() {
        var userId = currentUser.userId();
        var preference = caQueryService.handle(new GetNotificationPreferenceByUserIdQuery(userId))
                .orElseGet(() -> new NotificationPreference(userId));

        return ResponseEntity.ok(
                NotificationPreferenceResourceFromEntityAssembler.toResourceFromEntity(preference)
        );
    }

    @PutMapping(consumes = APPLICATION_JSON_VALUE)
    @Operation(summary = "Update the notification preferences of the authenticated user",
            description = "inAppEnabled controls the bell; emailEnabled the e-mail notices, which are sent only for "
                    + "critical alerts (US84); minimumSeverity is the lowest alert severity notified. Quality decisions "
                    + "on batches reach the bell whatever the severity.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notification preferences updated",
                    content = @Content(schema = @Schema(implementation = NotificationPreferenceResource.class))),
            @ApiResponse(responseCode = "400", description = "Missing values or unknown severity",
                    content = @Content(schema = @Schema(implementation = ErrorResource.class)))
    })
    public ResponseEntity<?> updatePreferences(@RequestBody UpdateNotificationPreferenceResource resource) {
        var command = UpdateNotificationPreferenceCommandFromResourceAssembler.toCommandFromResource(
                currentUser.userId(),
                resource
        );
        var result = caCommandService.handle(command);

        return ResponseEntityAssembler.toResponseEntityFromResult(
                result,
                NotificationPreferenceResourceFromEntityAssembler::toResourceFromEntity,
                HttpStatus.OK
        );
    }
}
