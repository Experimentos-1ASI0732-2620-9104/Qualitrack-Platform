package com.iotech.qualitrack.platform.ca.interfaces.rest;

import com.iotech.qualitrack.platform.ca.application.commandservices.NotificationCommandService;
import com.iotech.qualitrack.platform.ca.application.queryservices.NotificationQueryService;
import com.iotech.qualitrack.platform.ca.domain.model.commands.MarkAllNotificationsAsReadCommand;
import com.iotech.qualitrack.platform.ca.domain.model.commands.MarkNotificationAsReadCommand;
import com.iotech.qualitrack.platform.ca.domain.model.queries.GetNotificationsQuery;
import com.iotech.qualitrack.platform.ca.domain.model.queries.GetUnreadNotificationCountQuery;
import com.iotech.qualitrack.platform.ca.interfaces.rest.resources.NotificationResource;
import com.iotech.qualitrack.platform.ca.interfaces.rest.resources.NotificationsReadResource;
import com.iotech.qualitrack.platform.ca.interfaces.rest.resources.UnreadNotificationsResource;
import com.iotech.qualitrack.platform.ca.interfaces.rest.transform.NotificationResourceFromEntityAssembler;
import com.iotech.qualitrack.platform.shared.application.security.CurrentUser;
import com.iotech.qualitrack.platform.shared.interfaces.rest.resources.ErrorResource;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * Notifications of the authenticated user: what happened to the alerts and batches of the laboratory that the person
 * did not do (US83). The web application shows them in the bell of the toolbar.
 */
@RestController
@RequestMapping(value = "/api/v1/users/me/notifications", produces = APPLICATION_JSON_VALUE)
public class NotificationController {

    private final NotificationCommandService notificationCommandService;
    private final NotificationQueryService notificationQueryService;
    private final CurrentUser currentUser;

    public NotificationController(NotificationCommandService notificationCommandService,
                                  NotificationQueryService notificationQueryService, CurrentUser currentUser) {
        this.notificationCommandService = notificationCommandService;
        this.notificationQueryService = notificationQueryService;
        this.currentUser = currentUser;
    }

    @GetMapping
    @Operation(summary = "Get the notifications of the authenticated user",
            description = "Newest first. unread=true leaves out the notifications already read; limit goes from 1 to 100.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notifications",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = NotificationResource.class)))),
            @ApiResponse(responseCode = "400", description = "Limit out of range",
                    content = @Content(schema = @Schema(implementation = ErrorResource.class)))
    })
    public ResponseEntity<List<NotificationResource>> getNotifications(
            @RequestParam(defaultValue = "false") boolean unread,
            @RequestParam(defaultValue = "20") int limit) {
        var notifications = notificationQueryService.handle(new GetNotificationsQuery(currentUser.userId(), unread, limit));
        return ResponseEntity.ok(notifications.stream().map(NotificationResourceFromEntityAssembler::toResourceFromEntity).toList());
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Count the unread notifications of the authenticated user",
            description = "The web application asks for it periodically to update the bell.")
    @ApiResponse(responseCode = "200", description = "Unread notifications",
            content = @Content(schema = @Schema(implementation = UnreadNotificationsResource.class)))
    public ResponseEntity<UnreadNotificationsResource> getUnreadCount() {
        return ResponseEntity.ok(new UnreadNotificationsResource(
                notificationQueryService.handle(new GetUnreadNotificationCountQuery(currentUser.userId()))));
    }

    @PostMapping("/{notificationId}/read-receipts")
    @Operation(summary = "Mark a notification as read", description = "Reading it again keeps the first date.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Notification read",
                    content = @Content(schema = @Schema(implementation = NotificationResource.class))),
            @ApiResponse(responseCode = "404", description = "Notification not found among those of the user",
                    content = @Content(schema = @Schema(implementation = ErrorResource.class)))
    })
    public ResponseEntity<?> markAsRead(@PathVariable Long notificationId) {
        return ResponseEntityAssembler.toResponseEntityFromResult(
                notificationCommandService.handle(new MarkNotificationAsReadCommand(currentUser.userId(), notificationId)),
                NotificationResourceFromEntityAssembler::toResourceFromEntity, HttpStatus.CREATED);
    }

    @PostMapping("/read-receipts")
    @Operation(summary = "Mark every notification as read")
    @ApiResponse(responseCode = "201", description = "Notifications read",
            content = @Content(schema = @Schema(implementation = NotificationsReadResource.class)))
    public ResponseEntity<NotificationsReadResource> markAllAsRead() {
        return ResponseEntity.status(HttpStatus.CREATED).body(new NotificationsReadResource(
                notificationCommandService.handle(new MarkAllNotificationsAsReadCommand(currentUser.userId()))));
    }
}
