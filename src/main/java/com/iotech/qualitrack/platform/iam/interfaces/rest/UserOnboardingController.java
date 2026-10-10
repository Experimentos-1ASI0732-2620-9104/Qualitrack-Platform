package com.iotech.qualitrack.platform.iam.interfaces.rest;

import com.iotech.qualitrack.platform.iam.application.queryservices.UserOnboardingQueryService;
import com.iotech.qualitrack.platform.iam.domain.model.queries.GetUserOnboardingQuery;
import com.iotech.qualitrack.platform.iam.infrastructure.authorization.sfs.model.UserDetailsImpl;
import com.iotech.qualitrack.platform.iam.interfaces.rest.resources.UserOnboardingResource;
import com.iotech.qualitrack.platform.iam.interfaces.rest.transform.UserOnboardingResourceAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/v1/users/me/onboarding", produces = "application/json")
public class UserOnboardingController {
    private final UserOnboardingQueryService service;

    public UserOnboardingController(UserOnboardingQueryService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Get current user's onboarding state",
            description = "Resolves subscription entitlement and laboratory from the authenticated user. "
                    + "nextStep is PASSWORD_CHANGE, SUBSCRIPTION, LABORATORY or READY.")
    @ApiResponse(responseCode = "200", description = "Authoritative onboarding state")
    @ApiResponse(responseCode = "401", description = "Authentication required")
    public ResponseEntity<UserOnboardingResource> getOnboarding(@AuthenticationPrincipal UserDetailsImpl user) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(
                UserOnboardingResourceAssembler.toResource(service.handle(new GetUserOnboardingQuery(user.getId()))));
    }
}
