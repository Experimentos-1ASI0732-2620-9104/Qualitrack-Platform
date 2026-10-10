package com.iotech.qualitrack.platform.ra.interfaces.rest.transform;

import com.iotech.qualitrack.platform.ra.domain.model.aggregates.AuditReport;
import com.iotech.qualitrack.platform.shared.application.result.ApplicationError;
import com.iotech.qualitrack.platform.shared.application.result.Result;
import com.iotech.qualitrack.platform.shared.interfaces.rest.transform.ResponseEntityAssembler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/**
 * Responses of the report generation endpoints: {@code 201 Created} with the stored report and a {@code Location}
 * header pointing to {@code /api/v1/reports/{reportId}}; its PDF or CSV is downloaded from
 * {@code /api/v1/reports/{reportId}/content} (TS87).
 */
public final class ReportResponseAssembler {
    private ReportResponseAssembler() {
    }

    public static ResponseEntity<?> toCreatedResponse(Result<AuditReport, ApplicationError> result) {
        return ResponseEntityAssembler.toCreatedResponseEntityAtLocation(result,
                AuditReportResourceFromEntityAssembler::toResourceFromEntity, ReportResponseAssembler::toUriFromEntity);
    }

    public static URI toUriFromEntity(AuditReport report) {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/reports/{reportId}")
                .buildAndExpand(report.getId())
                .toUri();
    }
}
