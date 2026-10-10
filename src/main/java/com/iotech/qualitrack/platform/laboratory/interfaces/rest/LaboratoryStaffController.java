package com.iotech.qualitrack.platform.laboratory.interfaces.rest;

import com.iotech.qualitrack.platform.laboratory.application.commandservices.StaffCommandService;
import com.iotech.qualitrack.platform.laboratory.application.queryservices.StaffQueryService;
import com.iotech.qualitrack.platform.laboratory.domain.model.commands.DeactivateStaffCommand;
import com.iotech.qualitrack.platform.laboratory.domain.model.queries.GetStaffByLabIdQuery;
import com.iotech.qualitrack.platform.laboratory.domain.model.queries.GetStaffMemberByIdQuery;
import com.iotech.qualitrack.platform.laboratory.interfaces.rest.resources.RegisterStaffResource;
import com.iotech.qualitrack.platform.laboratory.interfaces.rest.resources.RegisteredStaffResource;
import com.iotech.qualitrack.platform.laboratory.interfaces.rest.resources.StaffMemberResource;
import com.iotech.qualitrack.platform.laboratory.interfaces.rest.transform.RegisterStaffCommandFromResourceAssembler;
import com.iotech.qualitrack.platform.laboratory.interfaces.rest.transform.StaffResourceFromEntityAssembler;
import com.iotech.qualitrack.platform.shared.application.result.ApplicationError;
import com.iotech.qualitrack.platform.shared.interfaces.rest.resources.ErrorResource;
import com.iotech.qualitrack.platform.shared.interfaces.rest.transform.ErrorResponseAssembler;
import com.iotech.qualitrack.platform.shared.interfaces.rest.transform.ResponseEntityAssembler;
import io.swagger.v3.oas.annotations.Operation;
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

import java.util.Comparator;
import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * Staff of a laboratory and the accounts they use to sign in (US34, TS19).
 */
@RestController
@RequestMapping(value = "/api/v1/laboratories/{laboratoryId}/staff", produces = APPLICATION_JSON_VALUE)
@PreAuthorize("@tenantAccess.allows('laboratoryId', #laboratoryId)")
public class LaboratoryStaffController {
    private static final String QUALITY_ROLES =
            "@tenantAccess.allows('laboratoryId', #laboratoryId) and hasAnyAuthority('ROLE_QA_MANAGER', 'ROLE_ADMIN')";

    private final StaffCommandService staffCommandService;
    private final StaffQueryService staffQueryService;

    public LaboratoryStaffController(StaffCommandService staffCommandService, StaffQueryService staffQueryService) {
        this.staffCommandService = staffCommandService;
        this.staffQueryService = staffQueryService;
    }

    @PostMapping(consumes = APPLICATION_JSON_VALUE)
    @PreAuthorize(QUALITY_ROLES)
    @Operation(summary = "Register a staff member",
            description = "Registers an operator or auditor and creates the account they sign in with: the e-mail is the "
                    + "username and a temporary password is e-mailed. When e-mail is not configured, the temporary password "
                    + "is returned once so the quality manager can hand it over.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Staff member and account created",
                    content = @Content(schema = @Schema(implementation = RegisteredStaffResource.class))),
            @ApiResponse(responseCode = "400", description = "Invalid data", content = @Content(schema = @Schema(implementation = ErrorResource.class))),
            @ApiResponse(responseCode = "403", description = "Laboratory not available to the account or missing quality role"),
            @ApiResponse(responseCode = "409", description = "E-mail already registered", content = @Content(schema = @Schema(implementation = ErrorResource.class)))
    })
    public ResponseEntity<?> registerStaff(@PathVariable Long laboratoryId, @Valid @RequestBody RegisterStaffResource resource) {
        var command = RegisterStaffCommandFromResourceAssembler.toCommandFromResource(resource, laboratoryId);
        return ResponseEntityAssembler.toCreatedResponseEntityFromResult(staffCommandService.handle(command),
                StaffResourceFromEntityAssembler::toResourceFromRegistration, registered -> registered.staffMember().getId());
    }

    @GetMapping
    @Operation(summary = "List staff", description = "Staff members of the laboratory, by name; used to assign people to operations.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Staff of the laboratory",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = StaffMemberResource.class)))),
            @ApiResponse(responseCode = "403", description = "Laboratory not available to the account")
    })
    public ResponseEntity<List<StaffMemberResource>> getStaffByLaboratoryId(@PathVariable Long laboratoryId) {
        return ResponseEntity.ok(staffQueryService.handle(new GetStaffByLabIdQuery(laboratoryId)).stream()
                .sorted(Comparator.comparing(staff -> staff.getFullName().toLowerCase()))
                .map(StaffResourceFromEntityAssembler::toResourceFromEntity)
                .toList());
    }

    @GetMapping("/{staffId}")
    @Operation(summary = "Get a staff member", description = "One staff member of the laboratory.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Staff member found",
                    content = @Content(schema = @Schema(implementation = StaffMemberResource.class))),
            @ApiResponse(responseCode = "403", description = "Staff member not available to the account"),
            @ApiResponse(responseCode = "404", description = "Staff member not registered in the laboratory",
                    content = @Content(schema = @Schema(implementation = ErrorResource.class)))
    })
    public ResponseEntity<?> getStaffMember(@PathVariable Long laboratoryId, @PathVariable Long staffId) {
        return staffQueryService.handle(new GetStaffMemberByIdQuery(laboratoryId, staffId))
                .<ResponseEntity<?>>map(staff -> ResponseEntity.ok(StaffResourceFromEntityAssembler.toResourceFromEntity(staff)))
                .orElseGet(() -> ErrorResponseAssembler.toErrorResponseFromApplicationError(ApplicationError.notFound("StaffMember", staffId)));
    }

    @PostMapping("/{staffId}/deactivations")
    @PreAuthorize(QUALITY_ROLES)
    @Operation(summary = "Deactivate a staff member", description = "The staff member stays in the records but can no longer sign in.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Staff member deactivated",
                    content = @Content(schema = @Schema(implementation = StaffMemberResource.class))),
            @ApiResponse(responseCode = "403", description = "Staff member not available to the account or missing quality role"),
            @ApiResponse(responseCode = "404", description = "Staff member not registered in the laboratory",
                    content = @Content(schema = @Schema(implementation = ErrorResource.class))),
            @ApiResponse(responseCode = "409", description = "Staff member already inactive",
                    content = @Content(schema = @Schema(implementation = ErrorResource.class)))
    })
    public ResponseEntity<?> deactivateStaffMember(@PathVariable Long laboratoryId, @PathVariable Long staffId) {
        return ResponseEntityAssembler.toResponseEntityFromResult(
                staffCommandService.handle(new DeactivateStaffCommand(laboratoryId, staffId)),
                StaffResourceFromEntityAssembler::toResourceFromEntity, HttpStatus.CREATED);
    }
}
