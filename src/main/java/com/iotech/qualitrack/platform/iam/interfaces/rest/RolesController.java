package com.iotech.qualitrack.platform.iam.interfaces.rest;

import com.iotech.qualitrack.platform.iam.application.queryservices.RoleQueryService;
import com.iotech.qualitrack.platform.iam.domain.model.queries.GetAllRolesQuery;
import com.iotech.qualitrack.platform.iam.interfaces.rest.resources.RoleResource;
import com.iotech.qualitrack.platform.iam.interfaces.rest.transform.RoleResourceFromEntityAssembler;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * REST controller for IAM role operations.
 */
@RestController
@RequestMapping(value = "/api/v1/roles", produces = APPLICATION_JSON_VALUE)
public class RolesController {

    private final RoleQueryService roleQueryService;

    public RolesController(RoleQueryService roleQueryService) {
        this.roleQueryService = roleQueryService;
    }

    @GetMapping
    @Operation(summary = "Get roles", description = "Retrieves all IAM roles.")
    public ResponseEntity<List<RoleResource>> getRoles() {
        var roles = roleQueryService.handle(new GetAllRolesQuery());

        var resources = roles.stream()
                .map(RoleResourceFromEntityAssembler::toResourceFromEntity)
                .toList();

        return ResponseEntity.ok(resources);
    }
}