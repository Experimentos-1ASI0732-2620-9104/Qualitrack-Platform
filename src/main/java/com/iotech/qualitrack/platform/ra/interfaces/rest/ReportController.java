package com.iotech.qualitrack.platform.ra.interfaces.rest;

import com.iotech.qualitrack.platform.ra.application.queryservices.RaQueryService;
import com.iotech.qualitrack.platform.ra.application.queryservices.ReportDocumentQueryService;
import com.iotech.qualitrack.platform.ra.domain.model.queries.GetReportDocumentByIdQuery;
import com.iotech.qualitrack.platform.ra.domain.model.valueobjects.ReportFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import com.iotech.qualitrack.platform.ra.domain.model.queries.GetAuditReportByIdQuery;
import com.iotech.qualitrack.platform.ra.interfaces.rest.resources.AuditReportResource;
import com.iotech.qualitrack.platform.ra.interfaces.rest.transform.AuditReportResourceFromEntityAssembler;
import com.iotech.qualitrack.platform.shared.application.result.ApplicationError;
import com.iotech.qualitrack.platform.shared.interfaces.rest.transform.ErrorResponseAssembler;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * REST controller that exposes audit report resources.
 */
@RestController
@RequestMapping(value = "/api/v1/reports", produces = APPLICATION_JSON_VALUE)
public class ReportController {

    private final RaQueryService raQueryService;
    private final ReportDocumentQueryService documents;

    public ReportController(RaQueryService raQueryService, ReportDocumentQueryService documents) {
        this.raQueryService = raQueryService;
        this.documents = documents;
    }

    @GetMapping(value = "/{reportId}/content", produces = {"application/pdf", "text/csv"})
    @Operation(summary = "Download a stored report", description = "Returns the original PDF or CSV after checksum verification. "
            + "Requires access to the report laboratory. Returns 404 for legacy metadata without stored content.")
    public ResponseEntity<?> getReportContent(@PathVariable Long reportId) {
        var document = documents.handle(new GetReportDocumentByIdQuery(reportId));
        if (document.isEmpty()) return ErrorResponseAssembler.toErrorResponseFromApplicationError(
                ApplicationError.notFound("ReportContent", reportId));
        var value = document.get();
        var pdf = value.format() == ReportFormat.PDF;
        return ResponseEntity.ok().contentType(pdf ? MediaType.APPLICATION_PDF : MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("report-" + reportId + (pdf ? ".pdf" : ".csv")).build().toString())
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(value.content());
    }

    @GetMapping(value = "/{reportId}")
    @Operation(summary = "Get audit report by ID")
    public ResponseEntity<?> getAuditReportById(@PathVariable Long reportId) {
        return raQueryService.handle(new GetAuditReportByIdQuery(reportId))
                .<ResponseEntity<?>>map(report -> ResponseEntity.ok(AuditReportResourceFromEntityAssembler.toResourceFromEntity(report)))
                .orElseGet(() -> ErrorResponseAssembler.toErrorResponseFromApplicationError(
                        ApplicationError.notFound("AuditReport", reportId)));
    }
}
