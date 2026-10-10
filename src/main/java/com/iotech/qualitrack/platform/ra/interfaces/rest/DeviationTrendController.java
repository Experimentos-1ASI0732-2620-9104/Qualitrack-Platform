package com.iotech.qualitrack.platform.ra.interfaces.rest;

import com.iotech.qualitrack.platform.ra.application.queryservices.RaQueryService;
import com.iotech.qualitrack.platform.ra.domain.model.queries.GetDeviationTrendsByEnvironmentQuery;
import com.iotech.qualitrack.platform.ra.interfaces.rest.resources.DeviationTrendResource;
import com.iotech.qualitrack.platform.ra.interfaces.rest.transform.DeviationTrendResourceFromEntityAssembler;
import com.iotech.qualitrack.platform.ra.interfaces.rest.transform.ReportingPeriodAssembler;
import com.iotech.qualitrack.platform.shared.application.result.ApplicationError;
import com.iotech.qualitrack.platform.shared.interfaces.rest.resources.ErrorResource;
import com.iotech.qualitrack.platform.shared.interfaces.rest.transform.ErrorResponseAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
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
 * Deviation indicators of the variables of an environment (US94, TS82).
 */
@RestController
@RequestMapping(value = "/api/v1/laboratories/{laboratoryId}/environments/{environmentId}/deviation-trends",
        produces = APPLICATION_JSON_VALUE)
public class DeviationTrendController {

    private final RaQueryService raQueryService;

    public DeviationTrendController(RaQueryService raQueryService) {
        this.raQueryService = raQueryService;
    }

    @GetMapping
    @Operation(summary = "Get the deviation indicators of an environment",
            description = "Per device and variable of the environment in the period (by default the last 7 days, at most "
                    + "31 days): time in range (each evaluated reading keeps its condition until the next one; NORMAL time "
                    + "over the time between the first and last evaluated reading), deviations (readings worse than the "
                    + "previous one), critical deviations and the readings in time order.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Indicators per device and variable; empty without readings",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = DeviationTrendResource.class)))),
            @ApiResponse(responseCode = "400", description = "Invalid period",
                    content = @Content(schema = @Schema(implementation = ErrorResource.class))),
            @ApiResponse(responseCode = "403", description = "Environment not available to the account"),
            @ApiResponse(responseCode = "404", description = "Environment not found",
                    content = @Content(schema = @Schema(implementation = ErrorResource.class)))
    })
    public ResponseEntity<?> getDeviationTrends(
            @PathVariable Long laboratoryId, @PathVariable Long environmentId,
            @Parameter(description = "Start of the period (ISO-8601 with offset)") @RequestParam(required = false) String from,
            @Parameter(description = "End of the period (ISO-8601 with offset)") @RequestParam(required = false) String to) {
        var period = ReportingPeriodAssembler.toIndicatorPeriod(from, to);
        return raQueryService.handle(new GetDeviationTrendsByEnvironmentQuery(laboratoryId, environmentId, period))
                .<ResponseEntity<?>>map(trends -> ResponseEntity.ok(trends.stream()
                        .map(DeviationTrendResourceFromEntityAssembler::toResourceFromEntity).toList()))
                .orElseGet(() -> ErrorResponseAssembler.toErrorResponseFromApplicationError(
                        ApplicationError.notFound("Environment", environmentId)));
    }
}
