package com.iotech.qualitrack.platform.subscription.interfaces.rest;

import com.iotech.qualitrack.platform.subscription.application.queryservices.SubscriptionQueryService;
import com.iotech.qualitrack.platform.subscription.domain.model.queries.GetSubscriptionPlansQuery;
import com.iotech.qualitrack.platform.subscription.interfaces.rest.resources.SubscriptionPlanResource;
import com.iotech.qualitrack.platform.subscription.interfaces.rest.transform.SubscriptionPlanResourceFromEntityAssembler;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * REST controller that exposes subscription plan resources.
 */
@RestController
@RequestMapping(value = "/api/v1/subscription-plans", produces = APPLICATION_JSON_VALUE)
public class SubscriptionPlanController {

    private final SubscriptionQueryService subscriptionQueryService;

    public SubscriptionPlanController(SubscriptionQueryService subscriptionQueryService) {
        this.subscriptionQueryService = subscriptionQueryService;
    }

    @GetMapping
    @Operation(summary = "Get subscription plans")
    public ResponseEntity<List<SubscriptionPlanResource>> getPlans() {
        var plans = subscriptionQueryService.handle(new GetSubscriptionPlansQuery());

        var resources = plans.stream()
                .map(SubscriptionPlanResourceFromEntityAssembler::toResourceFromEntity)
                .toList();

        return ResponseEntity.ok(resources);
    }
}