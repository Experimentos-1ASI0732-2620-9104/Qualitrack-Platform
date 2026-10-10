package com.iotech.qualitrack.platform.ra.interfaces.rest;

import com.iotech.qualitrack.platform.ra.application.queryservices.RaQueryService;
import com.iotech.qualitrack.platform.ra.domain.model.queries.GetStaffActivityQuery;
import com.iotech.qualitrack.platform.ra.interfaces.rest.resources.AuditLogEntryResource;
import com.iotech.qualitrack.platform.ra.interfaces.rest.transform.AuditLogEntryResourceFromEntityAssembler;
import com.iotech.qualitrack.platform.shared.application.result.ApplicationError;
import com.iotech.qualitrack.platform.shared.interfaces.rest.resources.ErrorResource;
import com.iotech.qualitrack.platform.shared.interfaces.rest.transform.ErrorResponseAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * Activity of a staff member: the operations registered with their account (US92).
 */
@RestController
@RequestMapping(value = "/api/v1/laboratories/{laboratoryId}/staff/{staffId}/audit-logs", produces = APPLICATION_JSON_VALUE)
@PreAuthorize("@tenantAccess.allows('laboratoryId', #laboratoryId) and hasAnyAuthority('ROLE_QA_MANAGER', 'ROLE_ADMIN', 'ROLE_AUDITOR')")
public class StaffAuditLogController {
    private final RaQueryService raQueryService;

    public StaffAuditLogController(RaQueryService raQueryService) {
        this.raQueryService = raQueryService;
    }

    @GetMapping
    @Operation(summary = "Get the activity of a staff member",
            description = "Audit entries of the operations the staff member registered (maintenance, batches, inventory and others), newest first.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Activity of the staff member",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = AuditLogEntryResource.class)))),
            @ApiResponse(responseCode = "403", description = "Staff member not available to the account or missing quality or auditor role"),
            @ApiResponse(responseCode = "404", description = "Staff member not registered in the laboratory",
                    content = @Content(schema = @Schema(implementation = ErrorResource.class)))
    })
    public ResponseEntity<?> getStaffActivity(@PathVariable Long laboratoryId, @PathVariable Long staffId) {
        return raQueryService.handle(new GetStaffActivityQuery(laboratoryId, staffId))
                .<ResponseEntity<?>>map(entries -> ResponseEntity.ok(entries.stream()
                        .map(AuditLogEntryResourceFromEntityAssembler::toResourceFromEntity).toList()))
                .orElseGet(() -> ErrorResponseAssembler.toErrorResponseFromApplicationError(ApplicationError.notFound("StaffMember", staffId)));
    }
}
