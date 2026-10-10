package com.iotech.qualitrack.platform.ra.interfaces.rest;

import com.iotech.qualitrack.platform.ra.application.queryservices.RaQueryService;
import com.iotech.qualitrack.platform.ra.domain.model.queries.GetKpiDashboardByLaboratoryIdQuery;
import com.iotech.qualitrack.platform.ra.interfaces.rest.resources.KpiDashboardResource;
import com.iotech.qualitrack.platform.ra.interfaces.rest.transform.KpiDashboardResourceFromEntityAssembler;
import com.iotech.qualitrack.platform.ra.interfaces.rest.transform.ReportingPeriodAssembler;
import com.iotech.qualitrack.platform.shared.application.result.ApplicationError;
import com.iotech.qualitrack.platform.shared.interfaces.rest.resources.ErrorResource;
import com.iotech.qualitrack.platform.shared.interfaces.rest.transform.ErrorResponseAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * Indicators of a laboratory calculated from persisted records (US93, TS81).
 */
@RestController
@RequestMapping(value = "/api/v1/laboratories/{laboratoryId}/kpi-dashboards", produces = APPLICATION_JSON_VALUE)
public class KpiDashboardController {

    private final RaQueryService raQueryService;

    public KpiDashboardController(RaQueryService raQueryService) {
        this.raQueryService = raQueryService;
    }

    @GetMapping
    @Operation(summary = "Get the indicators of the laboratory",
            description = "Operational counts and the average, minimum and maximum of the readings of each device and "
                    + "metric of the environments in the period (by default the last 7 days, at most 31 days). Calculated "
                    + "from persisted records on request; a metric without readings has no summary.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Indicators of the laboratory",
                    content = @Content(schema = @Schema(implementation = KpiDashboardResource.class))),
            @ApiResponse(responseCode = "400", description = "Invalid period",
                    content = @Content(schema = @Schema(implementation = ErrorResource.class))),
            @ApiResponse(responseCode = "403", description = "Laboratory not available to the account"),
            @ApiResponse(responseCode = "404", description = "Environment not found in the laboratory",
                    content = @Content(schema = @Schema(implementation = ErrorResource.class)))
    })
    public ResponseEntity<?> getKpiDashboardByLaboratoryId(
            @PathVariable Long laboratoryId,
            @Parameter(description = "Start of the period (ISO-8601 with offset)") @RequestParam(required = false) String from,
            @Parameter(description = "End of the period (ISO-8601 with offset)") @RequestParam(required = false) String to,
            @Parameter(description = "Only this environment") @RequestParam(required = false) Long environmentId) {
        var period = ReportingPeriodAssembler.toIndicatorPeriod(from, to);
        return raQueryService.handle(new GetKpiDashboardByLaboratoryIdQuery(laboratoryId, environmentId, period))
                .<ResponseEntity<?>>map(dashboard -> ResponseEntity.ok(KpiDashboardResourceFromEntityAssembler.toResourceFromEntity(dashboard)))
                .orElseGet(() -> ErrorResponseAssembler.toErrorResponseFromApplicationError(
                        ApplicationError.notFound("Environment", environmentId)));
    }
}
