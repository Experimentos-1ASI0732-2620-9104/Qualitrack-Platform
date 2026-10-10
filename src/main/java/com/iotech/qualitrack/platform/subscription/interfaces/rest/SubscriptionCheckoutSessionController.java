package com.iotech.qualitrack.platform.subscription.interfaces.rest;

import com.iotech.qualitrack.platform.shared.interfaces.rest.transform.ResponseEntityAssembler;
import com.iotech.qualitrack.platform.subscription.application.commandservices.SubscriptionCommandService;
import com.iotech.qualitrack.platform.subscription.interfaces.rest.resources.CheckoutSessionResource;
import com.iotech.qualitrack.platform.subscription.interfaces.rest.resources.CreateCheckoutSessionResource;
import com.iotech.qualitrack.platform.subscription.interfaces.rest.transform.CreateCheckoutSessionCommandFromResourceAssembler;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * REST controller that exposes subscription checkout session resources.
 */
@RestController
@RequestMapping(value = "/api/v1/subscription-checkout-sessions", produces = APPLICATION_JSON_VALUE)
public class SubscriptionCheckoutSessionController {

    private final SubscriptionCommandService subscriptionCommandService;

    public SubscriptionCheckoutSessionController(SubscriptionCommandService subscriptionCommandService) {
        this.subscriptionCommandService = subscriptionCommandService;
    }

    @PostMapping(consumes = APPLICATION_JSON_VALUE)
    @Operation(summary = "Create subscription checkout session")
    public ResponseEntity<?> createCheckoutSession(@RequestBody CreateCheckoutSessionResource resource,
            @org.springframework.security.core.annotation.AuthenticationPrincipal
            com.iotech.qualitrack.platform.iam.infrastructure.authorization.sfs.model.UserDetailsImpl user) {
        if ((resource.userId() != null && !resource.userId().equals(user.getId()))
                || (resource.laboratoryId() != null && !resource.laboratoryId().equals(user.getLaboratoryId()))) {
            throw new org.springframework.security.access.AccessDeniedException("Checkout identity must match the authenticated user");
        }
        var command = CreateCheckoutSessionCommandFromResourceAssembler.toCommandFromResource(
                new CreateCheckoutSessionResource(user.getId(), user.getLaboratoryId(), resource.planCode(),
                        resource.billingCycle(), resource.successUrl(), resource.cancelUrl()));
        var result = subscriptionCommandService.handle(command);

        return ResponseEntityAssembler.toResponseEntityFromResult(
                result,
                CheckoutSessionResource::new,
                HttpStatus.CREATED
        );
    }
}
