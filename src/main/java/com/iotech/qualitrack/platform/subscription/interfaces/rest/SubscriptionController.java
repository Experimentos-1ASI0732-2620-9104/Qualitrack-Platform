package com.iotech.qualitrack.platform.subscription.interfaces.rest;

import com.iotech.qualitrack.platform.shared.application.result.ApplicationError;
import com.iotech.qualitrack.platform.shared.application.result.Result;
import com.iotech.qualitrack.platform.shared.application.security.CurrentUser;
import com.iotech.qualitrack.platform.shared.interfaces.rest.resources.ErrorResource;
import com.iotech.qualitrack.platform.shared.interfaces.rest.transform.ResponseEntityAssembler;
import com.iotech.qualitrack.platform.subscription.application.commandservices.SubscriptionCommandService;
import com.iotech.qualitrack.platform.subscription.application.queryservices.SubscriptionQueryService;
import com.iotech.qualitrack.platform.subscription.domain.model.aggregates.Subscription;
import com.iotech.qualitrack.platform.subscription.domain.model.commands.CancelSubscriptionCommand;
import com.iotech.qualitrack.platform.subscription.domain.model.queries.GetPaymentsBySubscriptionIdQuery;
import com.iotech.qualitrack.platform.subscription.domain.model.queries.GetSubscriptionByIdQuery;
import com.iotech.qualitrack.platform.subscription.interfaces.rest.resources.SubscriptionPaymentResource;
import com.iotech.qualitrack.platform.subscription.interfaces.rest.resources.SubscriptionResource;
import com.iotech.qualitrack.platform.subscription.interfaces.rest.transform.SubscriptionPaymentResourceFromEntityAssembler;
import com.iotech.qualitrack.platform.subscription.interfaces.rest.transform.SubscriptionResourceFromEntityAssembler;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * Payments and cancellation requests of a laboratory subscription (TS11).
 */
@RestController
@RequestMapping(value = "/api/v1/subscriptions", produces = APPLICATION_JSON_VALUE)
public class SubscriptionController {

    private final SubscriptionCommandService subscriptionCommandService;
    private final SubscriptionQueryService subscriptionQueryService;
    private final CurrentUser currentUser;

    public SubscriptionController(SubscriptionCommandService subscriptionCommandService,
                                  SubscriptionQueryService subscriptionQueryService, CurrentUser currentUser) {
        this.subscriptionCommandService = subscriptionCommandService;
        this.subscriptionQueryService = subscriptionQueryService;
        this.currentUser = currentUser;
    }

    @GetMapping("/{subscriptionId}/payments")
    @Operation(summary = "Get the payments of a subscription")
    public ResponseEntity<List<SubscriptionPaymentResource>> getPaymentsBySubscriptionId(@PathVariable Long subscriptionId) {
        var resources = subscriptionQueryService.handle(new GetPaymentsBySubscriptionIdQuery(subscriptionId)).stream()
                .map(SubscriptionPaymentResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
        return ResponseEntity.ok(resources);
    }

    @PostMapping("/{subscriptionId}/cancellation-requests")
    @Operation(summary = "Cancel the renewal of a subscription",
            description = "Registers the cancellation of the renewal requested by the authenticated quality manager (US23, "
                    + "TS11). The subscription keeps its access until currentPeriodEnd and is not charged again.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Renewal cancelled; cancelAtPeriodEnd is true",
                    content = @Content(schema = @Schema(implementation = SubscriptionResource.class))),
            @ApiResponse(responseCode = "403", description = "Subscription not available to the account"),
            @ApiResponse(responseCode = "404", description = "Subscription not found",
                    content = @Content(schema = @Schema(implementation = ErrorResource.class))),
            @ApiResponse(responseCode = "409", description = "The subscription is not active or its renewal is already cancelled",
                    content = @Content(schema = @Schema(implementation = ErrorResource.class)))
    })
    public ResponseEntity<?> requestCancellation(@PathVariable Long subscriptionId) {
        var result = subscriptionCommandService.handle(new CancelSubscriptionCommand(subscriptionId, currentUser.userId()))
                .flatMap(id -> subscriptionQueryService.handle(new GetSubscriptionByIdQuery(id))
                        .<Result<Subscription, ApplicationError>>map(Result::success)
                        .orElseGet(() -> Result.failure(ApplicationError.notFound("Subscription", id))));
        return ResponseEntityAssembler.toResponseEntityFromResult(result,
                SubscriptionResourceFromEntityAssembler::toResourceFromEntity, HttpStatus.CREATED);
    }
}
