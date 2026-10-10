package com.iotech.qualitrack.platform.subscription.interfaces.rest;

import com.iotech.qualitrack.platform.subscription.application.queryservices.SubscriptionQueryService;
import com.iotech.qualitrack.platform.subscription.domain.model.queries.GetBillingSummaryByLaboratoryIdQuery;
import com.iotech.qualitrack.platform.subscription.domain.model.valueobjects.SubscriptionStatus;
import com.iotech.qualitrack.platform.subscription.interfaces.rest.resources.SubscriptionResource;
import com.iotech.qualitrack.platform.subscription.interfaces.rest.transform.SubscriptionResourceFromEntityAssembler;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * Subscriptions of a laboratory (TS10).
 */
@RestController
@RequestMapping(value = "/api/v1/laboratories/{laboratoryId}/subscriptions", produces = APPLICATION_JSON_VALUE)
public class LaboratorySubscriptionController {

    private final SubscriptionQueryService subscriptionQueryService;

    public LaboratorySubscriptionController(SubscriptionQueryService subscriptionQueryService) {
        this.subscriptionQueryService = subscriptionQueryService;
    }

    @GetMapping
    @Operation(summary = "Get the subscriptions of a laboratory",
            description = "Newest first; status filters them, for example status=ACTIVE for the subscription in force.")
    public ResponseEntity<List<SubscriptionResource>> getLaboratorySubscriptions(
            @PathVariable Long laboratoryId,
            @RequestParam(required = false) SubscriptionStatus status) {
        var resources = subscriptionQueryService.handle(new GetBillingSummaryByLaboratoryIdQuery(laboratoryId)).stream()
                .filter(subscription -> status == null || status.equals(subscription.getStatus()))
                .map(SubscriptionResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
        return ResponseEntity.ok(resources);
    }
}
